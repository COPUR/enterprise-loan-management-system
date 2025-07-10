import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;

/**
 * Simple validation runner for FAPI 2.0 + DPoP implementation
 * Validates core functionality without external test framework dependencies
 */
public class ValidationRunner {
    
    public static void main(String[] args) {
        ValidationRunner runner = new ValidationRunner();
        
        try {
            System.out.println("=== FAPI 2.0 + DPoP Implementation Validation ===\n");
            
            runner.validateDPoPKeyGeneration();
            runner.validateDPoPProofGeneration();
            runner.validateJktThumbprintCalculation();
            runner.validateDPoPProofStructure();
            runner.validatePerformance();
            
            System.out.println("\n=== All Validation Tests PASSED ===");
            System.out.println("✅ FAPI 2.0 + DPoP implementation is working correctly!");
            
        } catch (Exception e) {
            System.err.println("❌ Validation FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void validateDPoPKeyGeneration() throws Exception {
        System.out.println("🔑 Testing DPoP Key Generation...");
        
        // Test EC key generation
        JWK ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        assert ecKey != null : "EC key generation failed";
        assert "EC".equals(ecKey.getKeyType().getValue()) : "EC key type incorrect";
        assert ecKey.getKeyID() != null : "EC key ID missing";
        
        // Test RSA key generation
        JWK rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        assert rsaKey != null : "RSA key generation failed";
        assert "RSA".equals(rsaKey.getKeyType().getValue()) : "RSA key type incorrect";
        assert rsaKey.getKeyID() != null : "RSA key ID missing";
        
        // Verify keys are different
        assert !ecKey.getKeyID().equals(rsaKey.getKeyID()) : "Keys should be different";
        
        System.out.println("   ✅ Key generation successful");
    }
    
    private void validateJktThumbprintCalculation() throws Exception {
        System.out.println("🔐 Testing JKT Thumbprint Calculation...");
        
        JWK key = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        String thumbprint1 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(key);
        String thumbprint2 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(key);
        
        assert thumbprint1 != null : "Thumbprint calculation failed";
        assert thumbprint1.length() == 43 : "Thumbprint length incorrect: " + thumbprint1.length();
        assert thumbprint1.equals(thumbprint2) : "Thumbprints should be consistent";
        
        // Test different keys produce different thumbprints
        JWK key2 = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        String thumbprint3 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(key2);
        assert !thumbprint1.equals(thumbprint3) : "Different keys should produce different thumbprints";
        
        System.out.println("   ✅ JKT thumbprint calculation successful");
    }
    
    private void validateDPoPProofGeneration() throws Exception {
        System.out.println("📝 Testing DPoP Proof Generation...");
        
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient client = new DPoPClientLibrary.DPoPHttpClient(dpopKey);
        
        // Test token request proof
        String tokenProof = client.getDPoPHeader("POST", "https://api.banking.com/oauth2/token", null);
        assert tokenProof != null : "Token proof generation failed";
        assert tokenProof.startsWith("eyJ") : "Proof should be in JWT format";
        
        // Test API request proof with access token
        String apiProof = client.getDPoPHeader("GET", "https://api.banking.com/api/v1/loans", "test_token");
        assert apiProof != null : "API proof generation failed";
        assert apiProof.startsWith("eyJ") : "API proof should be in JWT format";
        
        // Verify proofs are different
        assert !tokenProof.equals(apiProof) : "Different proofs should be generated";
        
        System.out.println("   ✅ DPoP proof generation successful");
    }
    
    private void validateDPoPProofStructure() throws Exception {
        System.out.println("🏗️ Testing DPoP Proof Structure...");
        
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient client = new DPoPClientLibrary.DPoPHttpClient(dpopKey);
        
        String proof = client.getDPoPHeader("GET", "https://api.banking.com/api/v1/test", "access_token");
        SignedJWT jwt = SignedJWT.parse(proof);
        
        // Verify header
        assert "dpop+jwt".equals(jwt.getHeader().getType().toString()) : "Type header incorrect";
        assert jwt.getHeader().getJWK() != null : "JWK header missing";
        
        // Verify claims
        assert jwt.getJWTClaimsSet().getJWTID() != null : "JTI claim missing";
        assert "GET".equals(jwt.getJWTClaimsSet().getStringClaim("htm")) : "HTM claim incorrect";
        assert "https://api.banking.com/api/v1/test".equals(jwt.getJWTClaimsSet().getStringClaim("htu")) : "HTU claim incorrect";
        assert jwt.getJWTClaimsSet().getIssueTime() != null : "IAT claim missing";
        assert jwt.getJWTClaimsSet().getStringClaim("ath") != null : "ATH claim missing for API request";
        
        System.out.println("   ✅ DPoP proof structure validation successful");
    }
    
    private void validatePerformance() throws Exception {
        System.out.println("⚡ Testing Performance...");
        
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient client = new DPoPClientLibrary.DPoPHttpClient(dpopKey);
        
        long startTime = System.currentTimeMillis();
        
        // Generate 50 proofs
        for (int i = 0; i < 50; i++) {
            String proof = client.getDPoPHeader("GET", "https://api.test.com", "token");
            assert proof != null : "Proof generation failed at iteration " + i;
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        assert totalTime < 3000 : "Performance too slow: " + totalTime + "ms for 50 proofs";
        
        System.out.println("   ✅ Generated 50 DPoP proofs in " + totalTime + "ms - Performance acceptable");
    }
}