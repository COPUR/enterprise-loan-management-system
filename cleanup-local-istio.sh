#\!/bin/bash
echo "🧹 Cleaning up local Istio environment..."

# Stop port forwarding
pkill -f "kubectl.*port-forward" || true
echo "✓ Port forwarding stopped"

# Delete kind cluster
kind delete cluster --name banking-local || true
echo "✓ Kubernetes cluster deleted"

# Clean up temporary files
rm -f port-forward-services.sh status-local-istio.sh istio-gateway.yaml postgres-deployment.yaml banking-app-deployment.yaml
echo "✓ Temporary files cleaned"

echo ""
echo "✅ Local Istio environment cleanup complete\!"
echo "Note: If you added entries to /etc/hosts, you may want to remove them manually."
