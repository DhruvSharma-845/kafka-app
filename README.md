# Kafka System

A distributed messaging system built with **Apache Kafka**, **Spring Boot**, and **Kubernetes**. This project demonstrates a producer-consumer architecture running on a local Kind cluster.

```
┌─────────────────────────────────────────────────────────────┐
│                     Kind Cluster                            │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │   Producer  │───▶│    Kafka    │◀───│   Consumer  │     │
│  │   (8080)    │    │   (9092)    │    │   (8080)    │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Component     | Technology                |
| ------------- | ------------------------- |
| Runtime       | Java 21                   |
| Framework     | Spring Boot 3.5.8         |
| Messaging     | Apache Kafka (KRaft mode) |
| Orchestration | Kubernetes via Kind       |
| Build         | Maven, Docker             |

---

## Prerequisites

Before starting, ensure you have the following installed:

- **Docker** — [Install Docker](https://docs.docker.com/get-docker/)
- **Kind** — [Install Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation)
- **kubectl** — [Install kubectl](https://kubernetes.io/docs/tasks/tools/)
- **Java 21** (optional, for local development)
- **Maven** (optional, for local development)

---

## Quick Start

### 1. Create the Kind Cluster

```bash
kind create cluster --name kafka-cluster --config app-cluster/kind-config.yaml
```

This creates a cluster with:

- 1 control-plane node
- 3 worker nodes
- Port mapping: `localhost:9092` → Kafka

### 2. Create the Namespace

```bash
kubectl apply -f app-cluster/k8s-namespace.yaml
```

### 3. Deploy Kafka

```bash
# Deploy PVC, Service, and StatefulSet
kubectl apply -f app-cluster/kafka/configmap.yaml
kubectl apply -f app-cluster/kafka/pvc.yaml
kubectl apply -f app-cluster/kafka/service.yaml
kubectl apply -f app-cluster/kafka/statefulset.yaml

```

### 4. Build Docker Images

```bash
# Build Producer
podman build -t producer-app:latest ./producer
podman save producer-app:latest -o producer-app.tar

# Build Consumer
podman build -t consumer-app:latest ./consumer
podman save consumer-app:latest -o consumer-app.tar
```

### 5. Load Images into Kind

```bash
kind load image-archive producer-app.tar --name app-cluster
kind load image-archive consumer-app.tar --name app-cluster
```

### 6. Deploy Applications

```bash
kubectl apply -f app-cluster/apps/producer-deployment.yaml
kubectl apply -f app-cluster/apps/consumer-deployment.yaml

# Verify deployments
kubectl get pods -n app-cluster
```

---

## API Reference

### Producer Service

**Publish a message to a topic**

```http
POST /api/kafka/publish/{topic}
Content-Type: application/json

{
  "key": "message-key",
  "message": "Hello, Kafka!"
}
```

**Example:**

```bash
# Port-forward the producer
kubectl port-forward deploy/producer-app 8081:8080 -n app-cluster &

# Send a message
curl -X POST http://localhost:8080/api/kafka/publish/my-topic \
  -H "Content-Type: application/json" \
  -d '{"key": "key1", "message": "Hello from producer!"}'
```

### Consumer Service

**Consume messages from a topic**

```http
GET /api/kafka/consume/{topic}
```

**Example:**

```bash
# Port-forward the consumer (use different port)
kubectl port-forward deploy/consumer-app 8082:8080 -n app-cluster &

# Consume messages
curl http://localhost:8081/api/kafka/consume/my-topic
```

---

## Project Structure

```
kafka-system/
├── producer/                    # Producer Spring Boot app
│   ├── src/main/java/...
│   ├── Dockerfile
│   └── pom.xml
├── consumer/                    # Consumer Spring Boot app
│   ├── src/main/java/...
│   ├── Dockerfile
│   └── pom.xml
└── app-cluster/                 # Kubernetes manifests
    ├── kind-config.yaml         # Kind cluster configuration
    ├── k8s-namespace.yaml       # Namespace definition
    ├── kafka/
    │   ├── statefulset.yaml     # Kafka StatefulSet
    │   ├── service.yaml         # Kafka Services
    │   └── pvc.yaml             # Persistent Volume Claim
    └── apps/
        ├── producer-deployment.yaml
        └── consumer-deployment.yaml
```

---

## Useful Commands

### Cluster Management

```bash
# View cluster info
kubectl cluster-info --context kind-kafka-cluster

# List all pods
kubectl get pods -n app-cluster

# View pod logs
kubectl logs -f deploy/producer-app -n app-cluster
kubectl logs -f deploy/consumer-app -n app-cluster
kubectl logs -f kafka-0 -n app-cluster
```

### Kafka Operations

```bash
# Exec into Kafka pod
kubectl exec -it kafka-0 -n app-cluster -- bash

# List topics (inside Kafka pod)
/opt/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Create a topic
/opt/kafka/bin/kafka-topics.sh --create --topic my-topic \
  --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Describe a topic
/opt/kafka/bin/kafka-topics.sh --describe --topic my-topic \
  --bootstrap-server localhost:9092
```

### Cleanup

```bash
# Delete deployments
kubectl delete -f app-cluster/apps/

# Delete Kafka
kubectl delete -f app-cluster/kafka/

# Delete namespace
kubectl delete -f app-cluster/k8s-namespace.yaml

# Delete Kind cluster
kind delete cluster --name kafka-cluster
```

---

## Troubleshooting

| Issue                   | Solution                                                         |
| ----------------------- | ---------------------------------------------------------------- |
| Pods stuck in `Pending` | Check PVC binding: `kubectl get pvc -n app-cluster`              |
| Kafka not starting      | Check logs: `kubectl logs kafka-0 -n app-cluster`                |
| Image pull errors       | Ensure images are loaded: `kind load docker-image ...`           |
| Connection refused      | Verify service endpoints: `kubectl get endpoints -n app-cluster` |

---

## Configuration

### Kafka Settings

The Kafka broker runs in **KRaft mode** (no ZooKeeper required) with the following configuration:

| Setting       | Value                                 |
| ------------- | ------------------------------------- |
| Node ID       | 1                                     |
| Listeners     | PLAINTEXT://:9092, CONTROLLER://:9093 |
| Log Directory | /var/lib/kafka/data                   |
| Storage       | 10Gi PVC                              |

### Application Properties

**Producer** (`producer/src/main/resources/application.properties`):

```properties
spring.kafka.bootstrap-servers=kafka-headless.app-cluster.svc.cluster.local:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

**Consumer** (`consumer/src/main/resources/application.properties`):

```properties
spring.kafka.bootstrap-servers=kafka-headless.app-cluster.svc.cluster.local:9092
spring.kafka.consumer.group-id=consumer-group
spring.kafka.consumer.auto-offset-reset=earliest
```

---

## License

MIT
