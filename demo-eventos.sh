#!/usr/bin/env bash
# =============================================================================
# demo-eventos.sh — Script de demonstração da Arquitetura Orientada a Eventos
# Diamond Aviação — TP4
#
# Uso: ./demo-eventos.sh
# =============================================================================

set -euo pipefail

# --- Cores ---
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# --- Diretórios ---
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DISCOVERY_DIR="$SCRIPT_DIR/services/discovery-server"
CLIENT_DIR="$SCRIPT_DIR/services/client-service"
LEAD_DIR="$SCRIPT_DIR"

# --- PIDs ---
PIDS_DIR="$SCRIPT_DIR/.pids"
mkdir -p "$PIDS_DIR"

# --- Funções auxiliares ---
print_header() {
    echo ""
    echo -e "${BOLD}${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}${BLUE}  $1${NC}"
    echo -e "${BOLD}${BLUE}════════════════════════════════════════════════════════════════${NC}"
    echo ""
}

print_step() {
    echo -e "${CYAN}▸ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✔ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_error() {
    echo -e "${RED}✘ $1${NC}"
}

print_scenario() {
    echo ""
    echo -e "${BOLD}${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BOLD}${YELLOW}  CENÁRIO $1${NC}"
    echo -e "${BOLD}${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
}

pause() {
    echo ""
    echo -e "${BOLD}Pressione ENTER para continuar...${NC}"
    read -r
}

json_pretty() {
    python3 -m json.tool 2>/dev/null || cat
}

wait_for_service() {
    local url="$1"
    local name="$2"
    local max_attempts=30
    local attempt=0
    
    print_step "Aguardando $name ficar disponível em $url..."
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            print_success "$name está pronto!"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done
    print_error "$name não respondeu após ${max_attempts} tentativas."
    return 1
}

# --- Limpeza ao sair ---
cleanup() {
    print_header "LIMPEZA"
    
    print_step "Parando serviços Spring Boot..."
    for pidfile in "$PIDS_DIR"/*.pid; do
        if [ -f "$pidfile" ]; then
            local pid
            pid=$(cat "$pidfile")
            if kill -0 "$pid" 2>/dev/null; then
                kill "$pid" 2>/dev/null || true
                print_success "Processo $pid encerrado ($(basename "$pidfile" .pid))"
            fi
            rm -f "$pidfile"
        fi
    done
    
    print_step "Parando RabbitMQ..."
    cd "$SCRIPT_DIR"
    docker compose down 2>/dev/null || docker-compose down 2>/dev/null || true
    print_success "RabbitMQ parado."
    
    # Limpar dados H2
    rm -rf "$LEAD_DIR/data" "$CLIENT_DIR/data" "$DISCOVERY_DIR/data" 2>/dev/null || true
    
    echo ""
    print_success "Limpeza concluída!"
}

# --- INÍCIO ---
print_header "DEMONSTRAÇÃO — Arquitetura Orientada a Eventos com RabbitMQ"
echo -e "  ${BOLD}Diamond Aviação — TP4${NC}"
echo -e "  Padrões: Command Message | Event Notification | Event-Carried State Transfer"
echo ""

# --- Verificar Docker / Colima ---
print_step "Verificando Docker (Colima)..."
if colima status 2>/dev/null | grep -q "Running"; then
    print_success "Colima está rodando."
elif docker info > /dev/null 2>&1; then
    print_success "Docker está disponível."
else
    print_error "Docker/Colima não está rodando. Inicie com: colima start"
    exit 1
fi

# --- Limpar dados anteriores ---
print_step "Limpando dados de execuções anteriores..."
rm -rf "$LEAD_DIR/data" "$CLIENT_DIR/data" 2>/dev/null || true

# --- Subir RabbitMQ ---
print_header "1. SUBINDO RABBITMQ"
cd "$SCRIPT_DIR"
docker compose up -d 2>/dev/null || docker-compose up -d
wait_for_service "http://localhost:15672" "RabbitMQ Management UI"
echo ""
echo -e "  ${BOLD}Management UI:${NC} http://localhost:15672"
echo -e "  ${BOLD}Usuário:${NC} diamond"
echo -e "  ${BOLD}Senha:${NC} diamond123"

pause

# --- Subir serviços ---
print_header "2. SUBINDO SERVIÇOS SPRING BOOT"

print_step "Iniciando Discovery Server (porta 8761)..."
cd "$DISCOVERY_DIR"
mvn -q spring-boot:run > /tmp/discovery-server.log 2>&1 &
echo $! > "$PIDS_DIR/discovery-server.pid"
wait_for_service "http://localhost:8761" "Discovery Server"

print_step "Iniciando Client Service (porta 8081)..."
cd "$CLIENT_DIR"
mvn -q spring-boot:run > /tmp/client-service.log 2>&1 &
echo $! > "$PIDS_DIR/client-service.pid"
wait_for_service "http://localhost:8081/api/clients" "Client Service"

print_step "Iniciando Lead Service (porta 8080)..."
cd "$LEAD_DIR"
mvn -q spring-boot:run > /tmp/lead-service.log 2>&1 &
echo $! > "$PIDS_DIR/lead-service.pid"
wait_for_service "http://localhost:8080/api/leads" "Lead Service"

print_success "Todos os serviços estão rodando!"
echo ""
echo -e "  ${BOLD}Logs:${NC}"
echo -e "    Discovery: /tmp/discovery-server.log"
echo -e "    Client:    /tmp/client-service.log"
echo -e "    Lead:      /tmp/lead-service.log"

pause

# =============================================================================
# CENÁRIO 1: Resolução de cliente via eventos (Command Message + ECST)
# =============================================================================
print_scenario "1: Resolução de cliente via eventos assíncronos"
echo -e "  ${BOLD}Padrão:${NC} Command Message + Event-Carried State Transfer"
echo -e "  ${BOLD}Fluxo:${NC} LeadService → publica lead.client.resolve → RabbitMQ"
echo -e "         → ClientService consome → findOrCreate → publica client.resolved"
echo -e "         → LeadService consome → atualiza lead.clientId"
echo ""

print_step "Criando um novo lead (POST /api/leads)..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/leads \
  -H "Content-Type: application/json" \
  -d '{
    "prefix": "PP-DEMO",
    "model": "PHENOM 300E",
    "typeLabel": "BIZJET",
    "tboDate": "2027-03-15",
    "cvaDate": "2027-04-10",
    "client": {
      "cnpj": "99887766000100",
      "name": "Demo Aviação Ltda",
      "phone": "11988887777"
    }
  }')

LEAD_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
CLIENT_ID_INITIAL=$(echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('clientId', 'null'))" 2>/dev/null)

echo ""
echo -e "${BOLD}Resposta (imediata):${NC}"
echo "$RESPONSE" | json_pretty
echo ""
echo -e "  ${YELLOW}▸ Observe: clientId = ${CLIENT_ID_INITIAL} (null neste momento)${NC}"
echo -e "  ${YELLOW}▸ O lead foi criado sem esperar o client-service!${NC}"
echo -e "  ${YELLOW}▸ O comando lead.client.resolve foi publicado no RabbitMQ.${NC}"

pause

print_step "Aguardando processamento assíncrono (3 segundos)..."
sleep 3

print_step "Buscando o lead novamente (GET /api/leads/$LEAD_ID)..."
RESPONSE_AFTER=$(curl -s http://localhost:8080/api/leads/"$LEAD_ID")
CLIENT_ID_AFTER=$(echo "$RESPONSE_AFTER" | python3 -c "import sys, json; print(json.load(sys.stdin).get('clientId', 'null'))" 2>/dev/null)

echo ""
echo -e "${BOLD}Resposta (após processamento do evento):${NC}"
echo "$RESPONSE_AFTER" | json_pretty
echo ""
echo -e "  ${GREEN}▸ Agora clientId = ${CLIENT_ID_AFTER} (preenchido pelo evento client.resolved!)${NC}"
echo -e "  ${GREEN}▸ Padrão Event-Carried State Transfer: o evento carregou todos os dados${NC}"
echo -e "  ${GREEN}▸ necessários para atualizar o lead sem precisar de query adicional.${NC}"

pause

# =============================================================================
# CENÁRIO 2: Notificação de mudança de status (Event Notification)
# =============================================================================
print_scenario "2: Notificação de mudança de status"
echo -e "  ${BOLD}Padrão:${NC} Event Notification"
echo -e "  ${BOLD}Fluxo:${NC} LeadService → publica lead.status-changed → lead.events (Topic Exchange)"
echo -e "         → LeadNotificationListener consome (routing key: lead.#)"
echo ""

print_step "Registrando parecer 'Fechado' no lead (POST /api/leads/$LEAD_ID/history)..."
HISTORY_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/leads/$LEAD_ID/history" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "Fechado",
    "note": "Negócio fechado com sucesso — demonstração TP4"
  }')

echo ""
echo -e "${BOLD}Resposta:${NC}"
echo "$HISTORY_RESPONSE" | json_pretty
echo ""
echo -e "  ${GREEN}▸ Status mudou de NOVO → FECHADO${NC}"
echo -e "  ${GREEN}▸ Evento lead.status-changed publicado no exchange lead.events${NC}"
echo -e "  ${GREEN}▸ LeadNotificationListener consumiu: [NOTIFICAÇÃO] Lead teve status alterado${NC}"
echo ""

print_step "Verificando nos logs do lead-service..."
echo ""
echo -e "${BOLD}Últimas linhas relevantes do log:${NC}"
grep -E "(status-changed|NOTIFICAÇÃO.*status)" /tmp/lead-service.log 2>/dev/null | tail -4 || echo "  (verifique manualmente: tail -20 /tmp/lead-service.log)"

pause

# =============================================================================
# CENÁRIO 3: Notificação de atribuição (Event Notification)
# =============================================================================
print_scenario "3: Notificação de atribuição de lead"
echo -e "  ${BOLD}Padrão:${NC} Event Notification"
echo -e "  ${BOLD}Fluxo:${NC} LeadService → publica lead.assigned → lead.events (Topic Exchange)"
echo -e "         → LeadNotificationListener consome (routing key: lead.#)"
echo ""

# Criar outro lead para demonstrar atribuição
print_step "Criando novo lead para demonstrar atribuição..."
RESPONSE2=$(curl -s -X POST http://localhost:8080/api/leads \
  -H "Content-Type: application/json" \
  -d '{
    "prefix": "PR-EVT",
    "model": "KING AIR 350",
    "typeLabel": "TURBOÉLICE",
    "tboDate": "2027-05-20",
    "cvaDate": "2027-06-15",
    "client": {
      "cnpj": "11223344000155",
      "name": "EventAir Brasil",
      "phone": "21977776666"
    }
  }')
LEAD_ID2=$(echo "$RESPONSE2" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

sleep 1

print_step "Atribuindo lead ao vendedor v1 (POST /api/leads/$LEAD_ID2/assignment)..."
ASSIGN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/leads/$LEAD_ID2/assignment" \
  -H "Content-Type: application/json" \
  -d '{
    "assignedTo": "v1",
    "assignedBy": "renata"
  }')

echo ""
echo -e "${BOLD}Resposta:${NC}"
echo "$ASSIGN_RESPONSE" | json_pretty
echo ""
echo -e "  ${GREEN}▸ Lead atribuído a v1 por renata${NC}"
echo -e "  ${GREEN}▸ Evento lead.assigned publicado no exchange lead.events${NC}"
echo -e "  ${GREEN}▸ LeadNotificationListener consumiu: [NOTIFICAÇÃO] Lead atribuído a vendedor${NC}"

print_step "Verificando nos logs do lead-service..."
echo ""
echo -e "${BOLD}Últimas linhas relevantes do log:${NC}"
grep -E "(lead.assigned|NOTIFICAÇÃO.*atribuído)" /tmp/lead-service.log 2>/dev/null | tail -4 || echo "  (verifique manualmente: tail -20 /tmp/lead-service.log)"

pause

# =============================================================================
# CENÁRIO 4: Resiliência — client-service offline
# =============================================================================
print_scenario "4: Resiliência — client-service offline"
echo -e "  ${BOLD}Demonstra:${NC} RabbitMQ como buffer — mensagens são armazenadas na fila"
echo -e "  quando o consumidor está indisponível e processadas ao retornar."
echo ""

print_step "Derrubando o client-service..."
CLIENT_PID=$(cat "$PIDS_DIR/client-service.pid" 2>/dev/null)
if [ -n "$CLIENT_PID" ] && kill -0 "$CLIENT_PID" 2>/dev/null; then
    kill "$CLIENT_PID"
    sleep 2
    print_success "Client-service parado (PID $CLIENT_PID)"
else
    print_warning "Client-service já estava parado"
fi

echo ""
print_step "Criando lead com client-service OFFLINE..."
RESPONSE3=$(curl -s -X POST http://localhost:8080/api/leads \
  -H "Content-Type: application/json" \
  -d '{
    "prefix": "PP-RES",
    "model": "CITATION CJ4",
    "typeLabel": "JATO LEVE",
    "tboDate": "2027-07-01",
    "cvaDate": "2027-08-15",
    "client": {
      "cnpj": "55667788000199",
      "name": "Resiliência Aero",
      "phone": "31966665555"
    }
  }')
LEAD_ID3=$(echo "$RESPONSE3" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

echo ""
echo -e "${BOLD}Resposta (com client-service offline):${NC}"
echo "$RESPONSE3" | json_pretty
echo ""
echo -e "  ${GREEN}▸ Lead criado com sucesso (201 Created) mesmo com client-service offline!${NC}"
echo -e "  ${YELLOW}▸ clientId = null (esperado — ninguém processou o comando ainda)${NC}"
echo -e "  ${YELLOW}▸ O comando lead.client.resolve está aguardando na fila do RabbitMQ${NC}"
echo ""

echo -e "  ${BOLD}Verifique no RabbitMQ Management UI:${NC} http://localhost:15672"
echo -e "  Na aba Queues → client.resolve.queue → 1 mensagem Ready"

pause

print_step "Reiniciando o client-service..."
cd "$CLIENT_DIR"
mvn -q spring-boot:run > /tmp/client-service.log 2>&1 &
echo $! > "$PIDS_DIR/client-service.pid"
wait_for_service "http://localhost:8081/api/clients" "Client Service"

echo ""
print_step "Aguardando processamento da mensagem na fila (5 segundos)..."
sleep 5

print_step "Buscando o lead (GET /api/leads/$LEAD_ID3)..."
RESPONSE3_AFTER=$(curl -s "http://localhost:8080/api/leads/$LEAD_ID3")
CLIENT_ID3_AFTER=$(echo "$RESPONSE3_AFTER" | python3 -c "import sys, json; print(json.load(sys.stdin).get('clientId', 'null'))" 2>/dev/null)

echo ""
echo -e "${BOLD}Resposta (após client-service voltar):${NC}"
echo "$RESPONSE3_AFTER" | json_pretty
echo ""
echo -e "  ${GREEN}▸ clientId = ${CLIENT_ID3_AFTER} (preenchido!)${NC}"
echo -e "  ${GREEN}▸ A mensagem ficou na fila enquanto o client-service estava offline${NC}"
echo -e "  ${GREEN}▸ e foi processada automaticamente quando ele voltou!${NC}"

pause

# =============================================================================
# RESUMO
# =============================================================================
print_header "RESUMO DA DEMONSTRAÇÃO"

echo -e "  ${GREEN}✔ Cenário 1:${NC} Resolução de cliente via eventos (Command + ECST)"
echo -e "  ${GREEN}✔ Cenário 2:${NC} Notificação de mudança de status (Event Notification)"
echo -e "  ${GREEN}✔ Cenário 3:${NC} Notificação de atribuição (Event Notification)"
echo -e "  ${GREEN}✔ Cenário 4:${NC} Resiliência com client-service offline"
echo ""
echo -e "  ${BOLD}Padrões demonstrados:${NC}"
echo -e "    1. Command Message (lead.client.resolve)"
echo -e "    2. Event Notification (lead.status-changed, lead.assigned)"
echo -e "    3. Event-Carried State Transfer (client.resolved)"
echo ""
echo -e "  ${BOLD}Exchanges RabbitMQ:${NC}"
echo -e "    • lead.commands (Direct Exchange)"
echo -e "    • lead.events (Topic Exchange)"
echo -e "    • client.events (Topic Exchange)"
echo ""
echo -e "  ${BOLD}Abstrações Spring Boot:${NC}"
echo -e "    • RabbitTemplate + Jackson2JsonMessageConverter"
echo -e "    • @RabbitListener"
echo -e "    • @Configuration + @Bean (exchanges, queues, bindings)"
echo ""

echo -e "${YELLOW}Deseja encerrar todos os serviços? (s/n)${NC}"
read -r CLEANUP_ANSWER
if [[ "$CLEANUP_ANSWER" =~ ^[sS]$ ]]; then
    cleanup
else
    echo ""
    echo -e "  ${BOLD}Serviços continuam rodando. Para parar manualmente:${NC}"
    echo -e "    kill \$(cat .pids/*.pid)"
    echo -e "    docker compose down"
    echo ""
fi

echo -e "${GREEN}${BOLD}Demonstração concluída!${NC}"
