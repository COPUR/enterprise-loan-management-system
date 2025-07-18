package com.bank.infrastructure.repository;

import com.bank.shared.kernel.domain.AggregateRoot;
import com.bank.infrastructure.caching.MultiLevelCacheService;
import jakarta.persistence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Optimized Repository Base Class
 * 
 * Provides high-performance repository operations with:
 * - Native SQL queries for complex operations
 * - Batch processing for bulk operations
 * - Multi-level caching integration
 * - Async operations for non-blocking calls
 * - Connection pool optimization
 * - Banking-specific query patterns
 */
@Transactional
public abstract class OptimizedRepositoryBase<T extends AggregateRoot<ID>, ID> {
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    protected final MultiLevelCacheService cacheService;
    protected final Class<T> entityClass;
    protected final String entityName;
    protected final String cacheType;
    
    // Batch processing configuration
    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int FETCH_SIZE = 1000;
    
    public OptimizedRepositoryBase(Class<T> entityClass, String cacheType, MultiLevelCacheService cacheService) {
        this.entityClass = entityClass;
        this.entityName = entityClass.getSimpleName();
        this.cacheType = cacheType;
        this.cacheService = cacheService;
    }
    
    /**
     * Find entity by ID with caching
     */
    public Optional<T> findById(ID id) {
        return cacheService.get(
            cacheType, 
            id.toString(), 
            entityClass,
            this::loadFromDatabase
        ).map(Optional::of).orElse(Optional.empty());
    }
    
    /**
     * Find entities by IDs with batch optimization
     */
    public List<T> findByIds(Collection<ID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        
        // Use native query for better performance with large ID lists
        Query query = entityManager.createNativeQuery(
            buildFindByIdsQuery(ids.size()), 
            entityClass
        );
        
        int paramIndex = 1;
        for (ID id : ids) {
            query.setParameter(paramIndex++, id);
        }
        
        @SuppressWarnings("unchecked")
        List<T> results = query.getResultList();
        
        // Cache individual results
        results.forEach(entity -> 
            cacheService.put(cacheType, entity.getId().toString(), entity)
        );
        
        return results;
    }
    
    /**
     * Save single entity with cache invalidation
     */
    public T save(T entity) {
        T savedEntity = entityManager.merge(entity);
        entityManager.flush();
        
        // Update cache
        cacheService.put(cacheType, savedEntity.getId().toString(), savedEntity);
        
        return savedEntity;
    }
    
    /**
     * Batch save entities with optimized flushing
     */
    public List<T> saveAll(Collection<T> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        
        List<T> savedEntities = new java.util.ArrayList<>();
        int count = 0;
        
        for (T entity : entities) {
            T savedEntity = entityManager.merge(entity);
            savedEntities.add(savedEntity);
            
            // Batch flush every DEFAULT_BATCH_SIZE entities
            if (++count % DEFAULT_BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        
        // Final flush
        entityManager.flush();
        entityManager.clear();
        
        // Update cache for all saved entities
        savedEntities.forEach(entity -> 
            cacheService.put(cacheType, entity.getId().toString(), entity)
        );
        
        return savedEntities;
    }
    
    /**
     * Delete entity with cache eviction
     */
    public void delete(T entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
        cacheService.evict(cacheType, entity.getId().toString());
    }
    
    /**
     * Delete by ID with cache eviction
     */
    public void deleteById(ID id) {
        Query query = entityManager.createQuery(
            "DELETE FROM " + entityName + " e WHERE e.id = :id"
        );
        query.setParameter("id", id);
        query.executeUpdate();
        
        cacheService.evict(cacheType, id.toString());
    }
    
    /**
     * Batch delete entities
     */
    public void deleteAll(Collection<T> entities) {
        if (entities.isEmpty()) {
            return;
        }
        
        // Extract IDs for batch deletion
        List<ID> ids = entities.stream()
            .map(AggregateRoot::getId)
            .toList();
        
        deleteByIds(ids);
    }
    
    /**
     * Batch delete by IDs
     */
    public void deleteByIds(Collection<ID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        
        // Use native query for efficient batch deletion
        Query query = entityManager.createNativeQuery(
            buildDeleteByIdsQuery(ids.size())
        );
        
        int paramIndex = 1;
        for (ID id : ids) {
            query.setParameter(paramIndex++, id);
        }
        
        query.executeUpdate();
        
        // Evict from cache
        ids.forEach(id -> cacheService.evict(cacheType, id.toString()));
    }
    
    /**
     * Check if entity exists
     */
    public boolean existsById(ID id) {
        // Try cache first
        Optional<T> cached = Optional.ofNullable(
            cacheService.get(cacheType, id.toString(), entityClass, null)
        );
        
        if (cached.isPresent()) {
            return true;
        }
        
        // Use optimized count query
        Query query = entityManager.createQuery(
            "SELECT COUNT(e) FROM " + entityName + " e WHERE e.id = :id"
        );
        query.setParameter("id", id);
        
        Long count = (Long) query.getSingleResult();
        return count > 0;
    }
    
    /**
     * Count all entities
     */
    public long count() {
        Query query = entityManager.createQuery(
            "SELECT COUNT(e) FROM " + entityName + " e"
        );
        return (Long) query.getSingleResult();
    }
    
    /**
     * Find all entities with pagination
     */
    public Page<T> findAll(Pageable pageable) {
        // Count query
        long total = count();
        
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        
        // Data query with pagination
        TypedQuery<T> query = entityManager.createQuery(
            "SELECT e FROM " + entityName + " e ORDER BY e.id",
            entityClass
        );
        
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        query.setHint("org.hibernate.fetchSize", FETCH_SIZE);
        
        List<T> content = query.getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }
    
    /**
     * Execute native query with parameters
     */
    protected List<T> executeNativeQuery(String sql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(sql, entityClass);
        
        parameters.forEach(query::setParameter);
        query.setHint("org.hibernate.fetchSize", FETCH_SIZE);
        
        @SuppressWarnings("unchecked")
        List<T> results = query.getResultList();
        
        return results;
    }
    
    /**
     * Execute native query returning single result
     */
    protected Optional<T> executeNativeQuerySingle(String sql, Map<String, Object> parameters) {
        try {
            Query query = entityManager.createNativeQuery(sql, entityClass);
            parameters.forEach(query::setParameter);
            
            @SuppressWarnings("unchecked")
            T result = (T) query.getSingleResult();
            
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Execute native update query
     */
    protected int executeNativeUpdate(String sql, Map<String, Object> parameters) {
        Query query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        
        return query.executeUpdate();
    }
    
    /**
     * Async operation support
     */
    public CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return CompletableFuture.supplyAsync(() -> findById(id));
    }
    
    /**
     * Async batch operation support
     */
    public CompletableFuture<List<T>> findByIdsAsync(Collection<ID> ids) {
        return CompletableFuture.supplyAsync(() -> findByIds(ids));
    }
    
    /**
     * Warm up cache with commonly accessed entities
     */
    public void warmUpCache(Collection<ID> ids) {
        List<T> entities = findByIds(ids);
        Map<String, Object> cacheData = new java.util.HashMap<>();
        
        entities.forEach(entity -> 
            cacheData.put(entity.getId().toString(), entity)
        );
        
        cacheService.warmUpCache(cacheType, cacheData);
    }
    
    /**
     * Clear cache for this entity type
     */
    public void clearCache() {
        cacheService.evictAll(cacheType);
    }
    
    /**
     * Get entity statistics
     */
    public EntityStatistics getStatistics() {
        Query countQuery = entityManager.createQuery(
            "SELECT COUNT(e) FROM " + entityName + " e"
        );
        Long totalCount = (Long) countQuery.getSingleResult();
        
        // Get cache statistics
        var cacheStats = cacheService.getCacheStats();
        
        return new EntityStatistics(
            entityName,
            totalCount,
            cacheStats.getL1HitRate(),
            cacheStats.getL2HitRate(),
            cacheStats.getOverallHitRate()
        );
    }
    
    // Abstract methods for subclasses to implement
    
    /**
     * Build native SQL query for finding entities by IDs
     */
    protected abstract String buildFindByIdsQuery(int idCount);
    
    /**
     * Build native SQL query for deleting entities by IDs
     */
    protected abstract String buildDeleteByIdsQuery(int idCount);
    
    /**
     * Get table name for native queries
     */
    protected abstract String getTableName();
    
    /**
     * Get ID column name for native queries
     */
    protected abstract String getIdColumnName();
    
    // Private helper methods
    
    private T loadFromDatabase(String id) {
        try {
            ID entityId = convertStringToId(id);
            return entityManager.find(entityClass, entityId);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Convert string ID to appropriate type
     */
    protected abstract ID convertStringToId(String id);
    
    /**
     * Entity statistics for monitoring
     */
    public static class EntityStatistics {
        private final String entityName;
        private final Long totalCount;
        private final double l1HitRate;
        private final double l2HitRate;
        private final double overallHitRate;
        
        public EntityStatistics(String entityName, Long totalCount, 
                               double l1HitRate, double l2HitRate, double overallHitRate) {
            this.entityName = entityName;
            this.totalCount = totalCount;
            this.l1HitRate = l1HitRate;
            this.l2HitRate = l2HitRate;
            this.overallHitRate = overallHitRate;
        }
        
        // Getters
        public String getEntityName() { return entityName; }
        public Long getTotalCount() { return totalCount; }
        public double getL1HitRate() { return l1HitRate; }
        public double getL2HitRate() { return l2HitRate; }
        public double getOverallHitRate() { return overallHitRate; }
    }
}