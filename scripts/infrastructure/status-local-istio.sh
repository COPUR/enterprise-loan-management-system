#\!/bin/bash
echo "📊 Local Istio Environment Status"
echo "=================================="

echo ""
echo "🔧 Cluster Status:"
kubectl cluster-info --context kind-banking-local 2>/dev/null || echo "❌ Cluster not running"

echo ""
echo "📦 Istio System Pods:"
kubectl get pods -n istio-system

echo ""
echo "🏦 Banking System Pods:"
kubectl get pods -n banking-system

echo ""
echo "🌐 Services:"
kubectl get svc -n banking-system
kubectl get svc -n istio-system | grep -E "(istio-ingressgateway|kiali|grafana|prometheus|jaeger)"

echo ""
echo "🚪 Istio Gateways:"
kubectl get gateway -n banking-system

echo ""
echo "🔄 Virtual Services:"
kubectl get virtualservice -n banking-system

echo ""
echo "📈 Destination Rules:"
kubectl get destinationrule -n banking-system

echo ""
echo "🔒 Security Policies:"
echo "- PeerAuthentication:"
kubectl get peerauthentication -n banking-system
echo "- AuthorizationPolicy:"
kubectl get authorizationpolicy -n banking-system
echo "- RequestAuthentication:"
kubectl get requestauthentication -n banking-system
