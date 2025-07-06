package com.bank.loan.loan.security.migration;

import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * DPoP Migration Tool for Existing Clients
 * Helps existing clients migrate from FAPI 1.0 + mTLS to FAPI 2.0 + DPoP
 */
public class DPoPMigrationTool {
    
    /**
     * Migration Configuration
     */
    public static class MigrationConfig {
        private String clientId;
        private String clientName;
        private String keyType = "EC"; // EC or RSA
        private String outputDirectory = "./dpop-migration";
        private boolean generateSampleCode = true;
        private boolean validateConfiguration = true;
        
        // Getters and setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public String getKeyType() { return keyType; }
        public void setKeyType(String keyType) { this.keyType = keyType; }
        
        public String getOutputDirectory() { return outputDirectory; }
        public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }
        
        public boolean isGenerateSampleCode() { return generateSampleCode; }
        public void setGenerateSampleCode(boolean generateSampleCode) { this.generateSampleCode = generateSampleCode; }
        
        public boolean isValidateConfiguration() { return validateConfiguration; }
        public void setValidateConfiguration(boolean validateConfiguration) { this.validateConfiguration = validateConfiguration; }
    }
    
    /**
     * Migration Result
     */
    public static class MigrationResult {
        private final boolean success;
        private final String message;
        private final JWK dpopKey;
        private final String jktThumbprint;
        private final Path outputPath;
        
        public MigrationResult(boolean success, String message, JWK dpopKey, String jktThumbprint, Path outputPath) {
            this.success = success;
            this.message = message;
            this.dpopKey = dpopKey;
            this.jktThumbprint = jktThumbprint;
            this.outputPath = outputPath;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public JWK getDpopKey() { return dpopKey; }
        public String getJktThumbprint() { return jktThumbprint; }
        public Path getOutputPath() { return outputPath; }
    }
    
    /**
     * Migrate a client from FAPI 1.0 + mTLS to FAPI 2.0 + DPoP
     */
    public static MigrationResult migrateClient(MigrationConfig config) {
        try {
            System.out.println("Starting DPoP migration for client: " + config.getClientId());
            
            // 1. Validate configuration
            if (config.isValidateConfiguration()) {
                validateMigrationConfig(config);
            }
            
            // 2. Generate DPoP key pair
            JWK dpopKey = generateDPoPKey(config.getKeyType());
            String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKey);
            
            System.out.println("Generated DPoP key pair (" + config.getKeyType() + ") with JKT: " + jktThumbprint);
            
            // 3. Create output directory
            Path outputDir = createOutputDirectory(config.getOutputDirectory(), config.getClientId());
            
            // 4. Save DPoP key pair
            saveDPoPKeyPair(dpopKey, outputDir);
            
            // 5. Generate configuration files
            generateConfigurationFiles(config, dpopKey, jktThumbprint, outputDir);
            
            // 6. Generate sample code if requested
            if (config.isGenerateSampleCode()) {
                generateSampleCode(config, dpopKey, outputDir);
            }
            
            // 7. Generate migration checklist
            generateMigrationChecklist(config, outputDir);
            
            System.out.println("Migration completed successfully!");
            System.out.println("Output directory: " + outputDir.toAbsolutePath());
            
            return new MigrationResult(true, "Migration completed successfully", 
                                     dpopKey, jktThumbprint, outputDir);
            
        } catch (Exception e) {
            String errorMessage = "Migration failed: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            return new MigrationResult(false, errorMessage, null, null, null);
        }
    }
    
    private static void validateMigrationConfig(MigrationConfig config) {
        if (config.getClientId() == null || config.getClientId().trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID is required");
        }
        
        if (config.getClientName() == null || config.getClientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Client name is required");
        }
        
        if (!"EC".equals(config.getKeyType()) && !"RSA".equals(config.getKeyType())) {
            throw new IllegalArgumentException("Key type must be 'EC' or 'RSA'");
        }
    }
    
    private static JWK generateDPoPKey(String keyType) throws JOSEException {
        if ("EC".equals(keyType)) {
            return DPoPClientLibrary.DPoPKeyManager.generateECKey();
        } else if ("RSA".equals(keyType)) {
            return DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + keyType);
        }
    }
    
    private static Path createOutputDirectory(String baseDir, String clientId) throws IOException {
        Path outputDir = Paths.get(baseDir, clientId + "-dpop-migration");
        Files.createDirectories(outputDir);
        return outputDir;
    }
    
    private static void saveDPoPKeyPair(JWK dpopKey, Path outputDir) throws IOException {
        // Save public key (JWK format)
        Path publicKeyPath = outputDir.resolve("dpop-public-key.jwk");
        Files.writeString(publicKeyPath, dpopKey.toPublicJWK().toJSONString());
        
        // Save private key (JWK format) - SECURE THIS FILE!
        Path privateKeyPath = outputDir.resolve("dpop-private-key.jwk");
        Files.writeString(privateKeyPath, dpopKey.toJSONString());
        
        // Create README for key management
        Path keyReadmePath = outputDir.resolve("KEY-MANAGEMENT-README.md");
        String keyReadme = """
                # DPoP Key Management
                
                ## Important Security Notes
                
                ⚠️ **CRITICAL SECURITY WARNING** ⚠️
                
                The `dpop-private-key.jwk` file contains your private DPoP key.
                
                **DO NOT:**
                - Share this file with anyone
                - Store it in version control
                - Include it in application packages
                - Send it over insecure channels
                
                **DO:**
                - Store it securely (encrypted storage, HSM, key vault)
                - Limit access to authorized personnel only
                - Create secure backups
                - Rotate keys periodically
                
                ## Files Generated
                
                - `dpop-public-key.jwk` - Public key (safe to share)
                - `dpop-private-key.jwk` - Private key (KEEP SECURE!)
                
                ## Next Steps
                
                1. Securely store the private key
                2. Configure your application to use the DPoP key
                3. Update client registration with the public key
                4. Test the integration
                """;
        Files.writeString(keyReadmePath, keyReadme);
        
        System.out.println("Saved DPoP key pair to: " + outputDir.toAbsolutePath());
    }
    
    private static void generateConfigurationFiles(MigrationConfig config, JWK dpopKey, 
                                                 String jktThumbprint, Path outputDir) throws IOException, JOSEException {
        
        // Generate application properties
        Path propsPath = outputDir.resolve("application-dpop.properties");
        Properties props = new Properties();
        props.setProperty("dpop.enabled", "true");
        props.setProperty("dpop.client.id", config.getClientId());
        props.setProperty("dpop.client.name", config.getClientName());
        props.setProperty("dpop.key.type", config.getKeyType());
        props.setProperty("dpop.key.thumbprint", jktThumbprint);
        props.setProperty("dpop.private.key.path", "./dpop-private-key.jwk");
        props.setProperty("fapi.version", "2.0");
        props.setProperty("oauth2.par.enabled", "true");
        props.setProperty("oauth2.pkce.required", "true");
        props.setProperty("client.authentication.method", "private_key_jwt");
        
        try (var writer = Files.newBufferedWriter(propsPath)) {
            props.store(writer, "DPoP Configuration for " + config.getClientName());
        }
        
        // Generate JSON configuration
        Path jsonConfigPath = outputDir.resolve("dpop-config.json");
        String jsonConfig = String.format("""
                {
                  "client_id": "%s",
                  "client_name": "%s",
                  "dpop": {
                    "enabled": true,
                    "key_type": "%s",
                    "jkt_thumbprint": "%s",
                    "private_key_path": "./dpop-private-key.jwk"
                  },
                  "fapi": {
                    "version": "2.0",
                    "par_required": true,
                    "pkce_required": true
                  },
                  "oauth2": {
                    "authorization_endpoint": "https://auth.example.com/oauth2/authorize",
                    "token_endpoint": "https://auth.example.com/oauth2/token",
                    "par_endpoint": "https://auth.example.com/oauth2/par",
                    "client_authentication_method": "private_key_jwt"
                  }
                }
                """, config.getClientId(), config.getClientName(), config.getKeyType(), jktThumbprint);
        
        Files.writeString(jsonConfigPath, jsonConfig);
        
        System.out.println("Generated configuration files");
    }
    
    private static void generateSampleCode(MigrationConfig config, JWK dpopKey, Path outputDir) throws IOException {
        
        // Generate Java sample code
        Path javaSamplePath = outputDir.resolve("DPoPClientExample.java");
        String javaSample = String.format("""
                import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
                import com.nimbusds.jose.jwk.JWK;
                
                /**
                 * DPoP Client Example for %s
                 * 
                 * This example shows how to integrate DPoP with your existing client application.
                 */
                public class DPoPClientExample {
                    
                    private final DPoPClientLibrary.DPoPHttpClient dpopClient;
                    private final String clientId = "%s";
                    
                    public DPoPClientExample() throws Exception {
                        // Load DPoP key pair from secure storage
                        JWK dpopKey = loadDPoPKey(); // Implement this method
                        this.dpopClient = new DPoPClientLibrary.DPoPHttpClient(dpopKey);
                    }
                    
                    /**
                     * Step 1: Create PAR request with DPoP JKT
                     */
                    public String createPARRequest() {
                        String jktThumbprint = dpopClient.getJktThumbprint();
                        
                        // Include JKT thumbprint in PAR request
                        String parRequestBody = String.format(
                            "client_id=%%s&redirect_uri=%%s&response_type=code&scope=%%s&" +
                            "code_challenge=%%s&code_challenge_method=S256&dpop_jkt=%%s",
                            clientId, 
                            "https://your-app.com/callback",
                            "openid loans payments",
                            "your-pkce-challenge",
                            jktThumbprint
                        );
                        
                        return parRequestBody;
                    }
                    
                    /**
                     * Step 2: Exchange authorization code for tokens with DPoP
                     */
                    public void exchangeCodeForTokens(String authorizationCode) {
                        String tokenEndpoint = "https://auth.example.com/oauth2/token";
                        
                        // Create DPoP proof for token request
                        String dpopProof = dpopClient.getDPoPHeader("POST", tokenEndpoint, null);
                        
                        // Make token request with DPoP header
                        // HTTP headers:
                        // Authorization: Basic <client_credentials>  // or use private_key_jwt
                        // DPoP: <dpop_proof>
                        // Content-Type: application/x-www-form-urlencoded
                        
                        String tokenRequestBody = String.format(
                            "grant_type=authorization_code&code=%%s&redirect_uri=%%s&" +
                            "code_verifier=%%s&client_assertion_type=%%s&client_assertion=%%s",
                            authorizationCode,
                            "https://your-app.com/callback",
                            "your-pkce-verifier",
                            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                            "your-client-assertion-jwt"
                        );
                        
                        // Make HTTP POST request with DPoP header
                        System.out.println("DPoP Header: " + dpopProof);
                        System.out.println("Request Body: " + tokenRequestBody);
                    }
                    
                    /**
                     * Step 3: Make API calls with DPoP-bound tokens
                     */
                    public void makeAPICall(String accessToken) {
                        String apiEndpoint = "https://api.example.com/loans";
                        
                        // Create DPoP proof for API request
                        String dpopProof = dpopClient.getDPoPHeader("GET", apiEndpoint, accessToken);
                        
                        // Make API request with DPoP header
                        // HTTP headers:
                        // Authorization: DPoP <access_token>
                        // DPoP: <dpop_proof>
                        
                        System.out.println("API Endpoint: " + apiEndpoint);
                        System.out.println("Authorization: DPoP " + accessToken);
                        System.out.println("DPoP Header: " + dpopProof);
                    }
                    
                    /**
                     * Load DPoP key from secure storage
                     * IMPLEMENT THIS METHOD according to your security requirements
                     */
                    private JWK loadDPoPKey() throws Exception {
                        // Option 1: Load from file (development only)
                        // return JWK.parse(Files.readString(Paths.get("dpop-private-key.jwk")));
                        
                        // Option 2: Load from environment variable
                        // return JWK.parse(System.getenv("DPOP_PRIVATE_KEY"));
                        
                        // Option 3: Load from secure key vault
                        // return yourKeyVaultService.getDPoPKey(clientId);
                        
                        throw new UnsupportedOperationException("Implement loadDPoPKey() method");
                    }
                }
                """, config.getClientName(), config.getClientId());
        
        Files.writeString(javaSamplePath, javaSample);
        
        // Generate JavaScript/Node.js sample
        Path jsSamplePath = outputDir.resolve("dpop-client-example.js");
        String jsSample = String.format("""
                const jose = require('jose');
                const crypto = require('crypto');
                
                /**
                 * DPoP Client Example for %s (Node.js)
                 */
                class DPoPClientExample {
                    constructor() {
                        this.clientId = '%s';
                        this.dpopKeyPair = null; // Load from secure storage
                    }
                    
                    /**
                     * Initialize DPoP client with key pair
                     */
                    async initialize() {
                        // Load DPoP key pair from secure storage
                        this.dpopKeyPair = await this.loadDPoPKey();
                    }
                    
                    /**
                     * Create DPoP proof
                     */
                    async createDPoPProof(httpMethod, httpUri, accessToken = null) {
                        const now = Math.floor(Date.now() / 1000);
                        const jti = crypto.randomUUID();
                        
                        const payload = {
                            jti: jti,
                            htm: httpMethod,
                            htu: httpUri,
                            iat: now
                        };
                        
                        // Add access token hash if provided
                        if (accessToken) {
                            const hash = crypto.createHash('sha256').update(accessToken).digest();
                            payload.ath = hash.toString('base64url');
                        }
                        
                        // Create JWT with DPoP key
                        const jwt = await new jose.SignJWT(payload)
                            .setProtectedHeader({ 
                                alg: 'ES256', 
                                typ: 'dpop+jwt',
                                jwk: await jose.exportJWK(this.dpopKeyPair.publicKey)
                            })
                            .sign(this.dpopKeyPair.privateKey);
                            
                        return jwt;
                    }
                    
                    /**
                     * Make PAR request
                     */
                    async createPARRequest() {
                        const jkt = await this.calculateJKT();
                        
                        const params = new URLSearchParams({
                            client_id: this.clientId,
                            redirect_uri: 'https://your-app.com/callback',
                            response_type: 'code',
                            scope: 'openid loans payments',
                            code_challenge: 'your-pkce-challenge',
                            code_challenge_method: 'S256',
                            dpop_jkt: jkt
                        });
                        
                        return params.toString();
                    }
                    
                    /**
                     * Exchange code for tokens
                     */
                    async exchangeCodeForTokens(authorizationCode) {
                        const tokenEndpoint = 'https://auth.example.com/oauth2/token';
                        const dpopProof = await this.createDPoPProof('POST', tokenEndpoint);
                        
                        const body = new URLSearchParams({
                            grant_type: 'authorization_code',
                            code: authorizationCode,
                            redirect_uri: 'https://your-app.com/callback',
                            code_verifier: 'your-pkce-verifier',
                            client_assertion_type: 'urn:ietf:params:oauth:client-assertion-type:jwt-bearer',
                            client_assertion: 'your-client-assertion-jwt'
                        });
                        
                        const response = await fetch(tokenEndpoint, {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/x-www-form-urlencoded',
                                'DPoP': dpopProof
                            },
                            body: body.toString()
                        });
                        
                        return await response.json();
                    }
                    
                    /**
                     * Make API call with DPoP
                     */
                    async makeAPICall(accessToken) {
                        const apiEndpoint = 'https://api.example.com/loans';
                        const dpopProof = await this.createDPoPProof('GET', apiEndpoint, accessToken);
                        
                        const response = await fetch(apiEndpoint, {
                            headers: {
                                'Authorization': `DPoP $${accessToken}`,
                                'DPoP': dpopProof
                            }
                        });
                        
                        return await response.json();
                    }
                    
                    /**
                     * Calculate JKT thumbprint
                     */
                    async calculateJKT() {
                        const jwk = await jose.exportJWK(this.dpopKeyPair.publicKey);
                        return await jose.calculateJwkThumbprint(jwk);
                    }
                    
                    /**
                     * Load DPoP key pair from secure storage
                     */
                    async loadDPoPKey() {
                        // IMPLEMENT THIS METHOD according to your security requirements
                        throw new Error('Implement loadDPoPKey() method');
                    }
                }
                
                module.exports = DPoPClientExample;
                """, config.getClientName(), config.getClientId());
        
        Files.writeString(jsSamplePath, jsSample);
        
        System.out.println("Generated sample code files");
    }
    
    private static void generateMigrationChecklist(MigrationConfig config, Path outputDir) throws IOException {
        Path checklistPath = outputDir.resolve("MIGRATION-CHECKLIST.md");
        String checklist = String.format("""
                # DPoP Migration Checklist for %s
                
                ## Pre-Migration Steps
                
                - [ ] Review FAPI 2.0 + DPoP specifications
                - [ ] Understand security implications of migration
                - [ ] Plan migration timeline
                - [ ] Set up development environment
                
                ## Technical Implementation
                
                ### 1. Server-Side Changes
                - [ ] Deploy FAPI 2.0 + DPoP enabled authorization server
                - [ ] Enable PAR (Pushed Authorization Requests) endpoint
                - [ ] Configure DPoP validation on resource servers
                - [ ] Update client registration with DPoP public key
                
                ### 2. Client-Side Changes
                - [ ] Generate DPoP key pair (DONE ✓)
                - [ ] Integrate DPoP client library
                - [ ] Update PAR requests to include `dpop_jkt`
                - [ ] Modify token requests to include DPoP proof
                - [ ] Update API calls to include DPoP proof and use `DPoP` token type
                - [ ] Implement proper DPoP key management
                
                ### 3. Security Configuration
                - [ ] Securely store DPoP private key
                - [ ] Implement key rotation procedures
                - [ ] Configure proper access controls
                - [ ] Set up monitoring and alerting
                
                ## Testing
                
                ### 4. Development Testing
                - [ ] Test PAR flow with DPoP JKT
                - [ ] Test authorization code exchange with DPoP
                - [ ] Test API calls with DPoP-bound tokens
                - [ ] Test error handling (invalid proofs, replay attacks)
                - [ ] Test nonce handling (if implemented)
                
                ### 5. Integration Testing
                - [ ] End-to-end flow testing
                - [ ] Performance testing
                - [ ] Security testing
                - [ ] Load testing
                
                ## Deployment
                
                ### 6. Staging Deployment
                - [ ] Deploy to staging environment
                - [ ] Run regression tests
                - [ ] Validate monitoring and metrics
                - [ ] Test rollback procedures
                
                ### 7. Production Deployment
                - [ ] Deploy during maintenance window
                - [ ] Monitor system health
                - [ ] Validate client connectivity
                - [ ] Monitor error rates and performance
                
                ## Post-Migration
                
                ### 8. Cleanup
                - [ ] Remove mTLS certificates (after migration period)
                - [ ] Update documentation
                - [ ] Train support teams
                - [ ] Archive old configuration
                
                ## Configuration Details
                
                - **Client ID**: %s
                - **DPoP Key Type**: %s
                - **Migration Date**: %s
                - **Files Generated**: See output directory
                
                ## Support Contacts
                
                - **Security Team**: security@yourbank.com
                - **API Team**: api-support@yourbank.com
                - **Infrastructure**: infrastructure@yourbank.com
                
                ## Important Notes
                
                ⚠️ **Security Reminders**:
                - Never share private keys
                - Implement proper key rotation
                - Monitor for security incidents
                - Follow least privilege principles
                
                📝 **Documentation**:
                - Update API documentation
                - Update client onboarding guides
                - Update security policies
                """, 
                config.getClientName(), 
                config.getClientId(),
                config.getKeyType(),
                java.time.LocalDate.now()
        );
        
        Files.writeString(checklistPath, checklist);
        System.out.println("Generated migration checklist");
    }
    
    /**
     * Main method for command-line usage
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DPoPMigrationTool <client_id> <client_name> [key_type] [output_dir]");
            System.out.println("  client_id:  OAuth2 client identifier");
            System.out.println("  client_name: Human-readable client name");
            System.out.println("  key_type:   EC or RSA (default: EC)");
            System.out.println("  output_dir: Output directory (default: ./dpop-migration)");
            return;
        }
        
        MigrationConfig config = new MigrationConfig();
        config.setClientId(args[0]);
        config.setClientName(args[1]);
        
        if (args.length > 2) {
            config.setKeyType(args[2]);
        }
        
        if (args.length > 3) {
            config.setOutputDirectory(args[3]);
        }
        
        MigrationResult result = migrateClient(config);
        
        if (result.isSuccess()) {
            System.out.println("\\nMigration completed successfully!");
            System.out.println("JKT Thumbprint: " + result.getJktThumbprint());
            System.out.println("Output Directory: " + result.getOutputPath().toAbsolutePath());
            System.exit(0);
        } else {
            System.err.println("\\nMigration failed: " + result.getMessage());
            System.exit(1);
        }
    }
}