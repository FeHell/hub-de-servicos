# Planejamento de notificações de oportunidades

## Base reaproveitável

O Android é nativo em Kotlin e usa um cliente HTTP autenticado. O backend Spring Boot já fornece conta, sessão, cadastro, papéis funcionais e auditoria. Desta etapa, o campo de município atual tem chave estrangeira para o código oficial do IBGE e horário da última observação. Não existe integração FCM/APNs, registro de instalações ou agendamento de campanhas nos módulos analisados. O serviço SMS continua dedicado ao fluxo já existente.

## Cadastro de instalações e preferências

- Android com FCM e um canal específico de oportunidades. No Android 13+, pedir `POST_NOTIFICATIONS` após explicar o benefício. Nos demais casos, consultar a disponibilidade do canal.
- iOS com APNs via FCM, capacidades e chaves APNs do ambiente correspondente, autorização do usuário e atualização dos tokens Apple/FCM. Um único transporte de backend atende as duas plataformas. Para Android sem Google Play Services, manter a caixa de entrada consultável pelo app.
- Criar `usuario_dispositivo` com vínculo autenticado à conta, identificador aleatório da instalação, plataforma, token, ambiente, atividade recente e revogação. Permitir vários dispositivos por usuário. Atualizar no login, na rotação do token e periodicamente, e desvincular no logout ou na troca de conta. Invalidar tokens permanentes recusados pelo provedor. Isolar desenvolvimento e produção.
- Criar preferências por categoria, opção de desativar oportunidades, municípios de interesse escolhidos pelo usuário e horários silenciosos. Permissão do sistema e preferências da plataforma são decisões distintas.
- Tratar tokens como dados restritos. Usar credenciais do provedor apenas no servidor, proteger os registros, minimizar retenção e omitir tokens, telefones e coordenadas dos logs. O payload pode levar um identificador da oportunidade e texto breve. O app consulta os detalhes com autenticação e valida que a conta atual tem acesso.

## Agendamento no monólito modular

1. A empresa habilitada publica uma oportunidade com uma ou várias vagas, município, requisitos, validade e data/hora desejada. O backend valida a autorização efetiva para publicar. A opção Empresa no cadastro expressa interesse.
2. Registrar a divulgação com estado e versão. Persistir o instante UTC em `timestamptz` e o fuso IANA escolhido para reapresentação. Validar horários ambíguos, inexistentes ou fora da validade da oportunidade. Preparar cancelamento, reagendamento e um intervalo permitido entre programação e envio.
3. Registrar em uma transação a revisão da oportunidade e o trabalho pendente em uma tabela outbox. Um processador periódico reivindica lotes vencidos via `FOR UPDATE SKIP LOCKED` e uma reserva com vencimento. Confirma a reserva antes da chamada externa e recupera trabalhos interrompidos. A periodicidade define a janela de tolerância do disparo, junto com o atraso de processamento. PostgreSQL é suficiente para o volume inicial. Avaliar um broker quando latência, concorrência ou volume justificarem.
4. No horário programado, selecionar trabalhadores elegíveis e gerar uma notificação no histórico por pessoa. Para cada dispositivo ativo, registrar um trabalho por `(divulgacao, dispositivo)` com chave única. Antes de enviar, revalidar cancelamento, preferências e vínculo da instalação. Uma mesma oportunidade com três vagas normalmente gera uma divulgação.
5. Entregar em lotes limitados pelo provedor. Configurar limites por empresa, município, usuário e janela de tempo, com possibilidade de revisão administrativa. Aplicar TTL coerente com a validade da vaga. Repetir apenas falhas transitórias com recuo exponencial, jitter e `Retry-After`. Encerrar falhas permanentes, expiração e limite de tentativas. A aceitação do FCM e a entrega ou abertura são eventos distintos. Uma falha entre envio e gravação pode causar nova tentativa. Use o identificador estável da notificação para deduplicação no app.
6. Registrar métricas de atraso da fila, seleção de destinatários, aceitos pelo provedor, falhas, reenvios e tokens inválidos. Auditar quem programou, editou ou cancelou, com identificadores e contagens. Acompanhar quotas e custo de filas, servidores, armazenamento e observabilidade. O FCM é listado como serviço sem cobrança direta pelo Firebase.

## Cidade e elegibilidade

A oportunidade carrega seu próprio código IBGE. A seleção usa código exato do município ou um conjunto de códigos de uma região cadastrada. Combinar com papel Trabalhador, cadastro ativo, preferências, categoria e outras regras que o produto definir. A localização atual informada pelo app é aproximada e pode ficar desatualizada ou estar errada perto de fronteiras. Antes do lançamento, definir janela de validade e se a preferência explícita de cidade prevalece. Pessoas sem cidade recente ou declarada podem consultar oportunidades no app. O agendamento é central e independe de localização em segundo plano.

## Ordem de implementação

1. Definir autorização efetiva de publicação, elegibilidade, duração da vaga e regras de consentimento.
2. Construir oportunidade, programação, cancelamento e caixa de entrada com o backend como fonte de verdade.
3. Implementar registro de dispositivos e preferências, configurar FCM Android e executar um piloto para uma cidade.
4. Introduzir o processador da outbox, controle de volume, deduplicação, monitoramento e testes de falha.
5. Adicionar APNs via FCM no iOS e testar separadamente permissões, renovação de token e deep link.
6. Se houver taxa por divulgação no futuro, ligar a autorização de publicação paga a uma referência da divulgação antes do agendamento. Reenvios técnicos reutilizam a mesma divulgação. A cobrança e suas regras são uma decisão de produto posterior.

## Referências oficiais

- [Android · Permissão de notificações](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Firebase · Registro e manutenção de tokens FCM](https://firebase.google.com/docs/cloud-messaging/manage-tokens)
- [Firebase · FCM nas plataformas Apple](https://firebase.google.com/docs/cloud-messaging/ios/get-started)
- [Firebase · Escala, quotas e erros](https://firebase.google.com/docs/cloud-messaging/scale-fcm)
- [Firebase · Preços](https://firebase.google.com/pricing)
- [IBGE · Códigos oficiais dos municípios](https://www.ibge.gov.br/explica/codigos-dos-municipios.php)
