#\!/bin/bash
echo "🔗 Setting up port forwarding for Istio services..."

# Kill existing port-forward processes
pkill -f "kubectl.*port-forward" || true
sleep 2

# Port forward Istio Ingress Gateway
kubectl port-forward svc/istio-ingressgateway 8080:80 -n istio-system > /dev/null 2>&1 &
echo "✓ Istio Gateway: http://localhost:8080"

# Port forward Kiali
kubectl port-forward svc/kiali 20001:20001 -n istio-system > /dev/null 2>&1 &
echo "✓ Kiali: http://localhost:20001"

# Port forward Grafana
kubectl port-forward svc/grafana 3000:3000 -n istio-system > /dev/null 2>&1 &
echo "✓ Grafana: http://localhost:3000"

# Port forward Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n istio-system > /dev/null 2>&1 &
echo "✓ Prometheus: http://localhost:9090"

# Port forward Jaeger
kubectl port-forward svc/tracing 16686:16686 -n istio-system > /dev/null 2>&1 &
echo "✓ Jaeger: http://localhost:16686"

# Port forward Banking Application directly
kubectl port-forward svc/enterprise-loan-system 8081:8080 -n banking-system > /dev/null 2>&1 &
echo "✓ Banking App (direct): http://localhost:8081"

echo ""
echo "🎯 Access URLs:"
echo "   Banking App via Istio: http://localhost:8080"
echo "   Banking App (direct): http://localhost:8081"
echo ""
echo "📊 Observability:"
echo "   Kiali (Service Mesh): http://localhost:20001"
echo "   Grafana (Metrics): http://localhost:3000"
echo "   Prometheus (Metrics): http://localhost:9090"
echo "   Jaeger (Tracing): http://localhost:16686"
echo ""
echo "Press Ctrl+C to stop port forwarding"
wait
