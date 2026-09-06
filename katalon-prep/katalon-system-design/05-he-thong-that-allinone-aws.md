# System design: all-in-one-v2 vs Katalon — review + câu hỏi phỏng vấn

> **Bản 2 (21/08/2026)** — sau khi bạn cập nhật full source, repo tăng từ 858 lên **~7800 file
> Java**. Review lại bằng 5 agent song song (kiến trúc, AWS, API Gateway, Auth, Workflow+Batch).
> **Ba kết luận chính ở bản 1 bị sai và đã sửa ở đây** — ghi lại rõ để bạn thấy chính bằng chứng
> thay đổi thế nào, không chỉ đọc bản mới:
>
> | Bản 1 (858 file, nhiều module rỗng) | Bản 2 (7800 file, checkout đầy đủ) |
> |---|---|
> | "API Gateway quy hoạch nhưng chưa xây" | ❌ Sai — gateway **thật**, Spring Cloud Gateway, có cross-cutting concern thật (dù thiếu rate-limit/circuit-breaker) |
> | "Batch chỉ có `@Scheduled` cron" | ❌ Sai — **Spring Batch 3.0.7 + Quartz thật**, 50+ job production |
> | "Không có multi-tenant SaaS thật, chỉ phân kênh" | ❌ Sai — **có cơ chế tenant isolation thật** qua `PartnerContext` + Postgres RLS, tách biệt với phân kênh |
>
> Mọi khẳng định dưới đây có bằng chứng file cụ thể. Phần nào vẫn chưa xác nhận được, tôi ghi rõ.
>
> **Lưu ý:** dự án này **không có trong CV** hiện tại của bạn
> ([CV-BASED-ANSWERS.md](../CV-BASED-ANSWERS.md), dựa trên `Duc-Nguyen-Minh-2026.docx`, không nhắc
> tên `all-in-one-v2`). Tự quyết định: nói cụ thể tên dự án, hay chỉ dùng **pattern** học được mà
> không cần gọi tên.

> **Bản 3 (26/08/2026)** — phát hiện ra `all-in-one-v2` chỉ là **app-repo**; hạ tầng thật nằm ở một
> **repo Git riêng biệt** (`fe-devops/commercial-infras`, remote `bitbucket.org/austincapitalbank/
> commercial-infras`, 6801 commit, commit gần nhất 07/2025 — không phải toy/demo). Review repo đó
> đã **xác nhận** nhiều thứ ở Bản 2 từng chỉ ghi "chưa xác nhận"/"suy luận cấu trúc":
>
> | Bản 2 (chỉ review app-repo) | Bản 3 (thêm review infra-repo riêng) |
> |---|---|
> | "ALB đứng trước Gateway route tĩnh — suy luận cấu trúc, **chưa** xác nhận" | ✅ **XÁC NHẬN** — `aws_lb` + `aws_lb_target_group` + health check + Route53 private-zone alias trỏ tới ALB DNS, có file:line thật |
> | "Autoscaling — không thấy trong repo, có thể nằm ngoài" | ✅ **XÁC NHẬN CÓ** — nhưng là **StepScaling theo CloudWatch alarm CPU/Memory**, KHÔNG phải TargetTracking hay `ALBRequestCountPerTarget` |
> | "Không có Terraform/CloudFormation/Helm/K8s nào trong toàn repo" | ⚠️ Chỉ đúng cho **app-repo**. Infra-repo riêng có 261+ file Terraform + một cụm **EKS thật** (nhưng ngừng cập nhật từ 08/2023, đã bị thay bằng ECS Fargate) |
> | "Không có SQS/SNS/DynamoDB/Lambda ở đâu cả" | ❌ **Sai cho Lambda + SNS** — có thật ở infra-repo (`elb-access-log-parser`, `endpoint-checker`, `sns2msteam`). SQS/DynamoDB (dữ liệu nghiệp vụ) vẫn không thấy |
> | "`commercial-infras/` không tồn tại trên đĩa" | ❌ Sai — tồn tại, chỉ ở **repo Git khác**, không nằm trong checkout `all-in-one-v2` |
>
> **Bài học quan trọng hơn mọi chi tiết AWS ở trên:** review một repo **không đủ** để kết luận
> "hệ thống không có X". Ranh giới **app-repo vs infra-repo** là điểm mù thật đã xảy ra — "không tìm
> thấy trong repo tôi đang xem" và "không tồn tại trong cả hệ thống" là hai câu khác nhau, và tôi đã
> lẫn hai câu đó ở Bản 2. Đây tự nó là một câu trả lời tốt cho câu hỏi "kể một lần review/audit sai
> và bạn phát hiện ra sao" — xem [mục 5.2 câu 17](#52-câu-hỏi-rèn-từ-chính-all-in-one-v2--dùng-được-câu-chuyện-thật-bản-2).

---

## 0. Giới hạn dữ liệu còn lại

Checkout lần này đầy đủ hơn nhiều, nhưng vẫn còn vài khoảng trống:

- ~~`commercial-infras/` (deployment-conf, jenkins-job-dsl-new) — không tồn tại trên đĩa~~ →
  **ĐÃ TÌM THẤY**, nhưng ở một **repo Git riêng** (`fe-devops/commercial-infras`), không nằm trong
  checkout `all-in-one-v2`. Xem [mục 1.8b](#18b-đính-chính-lớn-alb--autoscaling-đã-xác-nhận-bằng-terraform-thật-ở-repo-infra-riêng) và [1.9b](#19b-bổ-sung-aws-sau-khi-review-repo-infra-riêng--lambdasnsauroraredshiftwaf-thật-eks-thật-nhưng-đã-ngừng-dùng). Không có
  Terraform/CloudFormation/Helm/K8s nào trong **app-repo** — nhưng infra-repo riêng thì có đầy
  đủ (261+ file `.tf`, một cụm EKS thật đã ngừng dùng từ 08/2023).
- `workflow-service` — được gọi qua Feign từ nhiều module (`WorkflowFeignClient` → `http://localhost:9082`)
  nhưng **implementation của chính nó vẫn không có trong checkout** (chưa kiểm tra lại trong
  infra-repo — có thể vẫn là gap thật).
- Cơ chế deploy của 6 service dòng "NextGen" (`acb-credit`, `acb-kyc`, `acb-ng-account`,
  `acb-ng-core`, `acb-ng-customer`, `acb-ng-origination`) — **được xác nhận PHẦN NÀO** sau khi có
  infra-repo: `credit` và `ng-customer` xuất hiện trong `shared/tfvars/production.tfvars` với đầy
  đủ port/ALB/autoscaling giống các service Commercial khác (`credit:8092`, `ng-customer:8091`).
  4 service NextGen còn lại (`acb-kyc`, `acb-ng-account`, `acb-ng-core`, `acb-ng-origination`)
  **vẫn chưa xác nhận được** trong cả hai repo.
- Liên kết trực tiếp giữa `TenantHikariDataSource` (đặt `app.partners`) và các Postgres RLS policy
  thật (`V5_4__update_user_account_rls_policy_.sql`...) là suy luận hợp lý từ hai bằng chứng độc
  lập, **chưa grep được** một câu `CREATE POLICY ... USING (current_setting('app.partners')...)`
  cụ thể để nối trực tiếp hai đầu. Nói ở mức "rất có khả năng", không nói "chắc chắn 100%".
- **Giá trị route Gateway thật trong production KHÔNG nằm trong bất kỳ file YAML nào** — nó được
  inject lúc chạy từ AWS Secrets Manager (`spring.config.import: aws-secretsmanager:/buss/prod/gateway`,
  `application-profile.yml`). Terraform cho path đó cũng không có trong infra-repo (chỉ còn ở
  `archived/secrets-archived/`) — nên **chuỗi URI production thật** (ví dụ Gateway trỏ đúng string
  gì cho `auth-service`) vẫn là suy luận từ quy ước tên (Route53 record = tên service = key trong
  `service_conf`), không phải giá trị đã đọc trực tiếp.

---

## 1. System design thật của all-in-one-v2 — theo bằng chứng code

### 1.1 Kiến trúc tổng thể — hai product line song song, KHÔNG phải một cuộc di trú

Đây là sửa lớn nhất về cách hiểu tổng thể. Repo gồm **hai git repo riêng** cộng **4 shared lib**
đứng độc lập ở top-level:

```text
acb-ng-allinone/            (bitbucket: austincapitalbank/acb-ng-allinone)
├── acb-credit               → CreditApplication          — dòng "NextGen" (retail/consumer)
├── acb-credit-sdk           → lib, client Experian
├── acb-kyc                  → KycApplication
├── acb-ng-account           → AccountNextGenApplication
├── acb-ng-core               → lib (không main class)
├── acb-ng-customer          → CustomerNextGenApplication  (package com.acb.nextgen.customer)
└── commercial-svc-customer  → CustomerApplication          (package com.acb.commercial.customer)
                                ↑ CÙNG nằm trong repo này, nhưng là dòng "Commercial" khác

commercial-svc-allinone/    (repo riêng — dòng "Commercial" / business banking)
├── commercial-svc-account, -audit, -auth, -batch, -communication,
│   -core, -gateway, -origination, -reporting, acb-eoscar-sdk, acb-payment

top-level (shared lib, đứng ngoài cả hai repo):
├── acb-column-encryption-sdk   → mã hoá cột, dùng AWS KMS + Secrets Manager (SDK v2 — hiện đại)
├── acb-fuzzy-name-matcher
├── acb-workflow-lib            → AOP + SpEL rule evaluator (xem §1.5)
└── commercial-common-lib       → hạ tầng multi-tenant dùng chung (xem §1.4) ⭐
```

**`acb-ng-customer` và `commercial-svc-customer` KHÔNG phải bản cũ/mới của cùng một thứ** (bản 1
đoán sai đây là strangler-fig migration). Bằng chứng: package khác hẳn
(`com.acb.nextgen.customer` vs `com.acb.commercial.customer`), Flyway schema khác
(`nextgen` vs `public`), và **cả hai đều có commit gần nhau** (17-18/07/2025) — không cái nào bị
bỏ hoang. Đây là **hai product line chạy song song**: NextGen (ngân hàng bán lẻ/cá nhân) và
Commercial (ngân hàng doanh nghiệp), **chia sẻ cùng một Postgres instance nhưng khác schema**, và
chia sẻ hạ tầng chung (`commercial-common-lib`, `acb-workflow-lib`). Hai dòng này **có gọi chéo
nhau thật**: `commercial-svc-origination` có `NGCustomerClient`/`NGAccountClient` gọi trực tiếp
sang dòng NextGen.

### 1.2 Giao tiếp giữa service

- **Đồng bộ:** Feign vẫn là cơ chế chính, dùng cả trong-dòng và **xuyên dòng** (Commercial gọi
  NextGen qua `NGCustomerClient`/`NGAccountClient`). `WebClient` dùng ở gateway, origination,
  customer.
- **Bất đồng bộ:** Spring Cloud Stream + **RabbitMQ** (`shared-mq`) vẫn là broker duy nhất đang
  hoạt động. Hai dấu tích nợ kỹ thuật thật thú vị: một **ActiveMQ binder tuỳ biến** nằm trong
  `commercial-svc-customer` nhưng bị comment-out (`default-candidate: false`) — code chết, không
  xoá; và cấu hình **Kafka consumer/producer** (`bootstrap-servers: localhost:9092`) tồn tại trong
  `application-local.yml` của `commercial-svc-core`/`commercial-svc-account` nhưng **không có
  dependency `spring-kafka`** — cấu hình chết, Kafka chưa từng thật sự chạy.

### 1.3 Database — Postgres là chính, có thêm Redis và Elasticsearch

- **Postgres** cho persistence chính ở mọi service — không đổi so với bản 1.
- **Mới xác nhận:** `spring-boot-starter-data-redis` trong `commercial-svc-auth` và
  `commercial-svc-gateway` — nhiều khả năng cache session/token (khớp với `ForceLogoutRedisService`
  ở gateway, xem §1.8). **Elasticsearch** chỉ ở `commercial-svc-audit`.
- **"Database per service" vẫn không chặt** — nhưng giờ có nghĩa rõ hơn: một codebase
  (`commercial-svc-batch`, `commercial-svc-communication`) được **deploy hai lần qua Maven
  profile** `cons` (NextGen, port 5432) và `buss` (Commercial, port 5433), mỗi bản trỏ Flyway
  location riêng (`db/consumer` vs `db/business`). Đây là **schema/DB theo product line**, không
  phải theo service.

### 1.4 Multi-tenant SaaS — CÓ BẰNG CHỨNG THẬT ⭐ đính chính quan trọng nhất

Bản 1 cảnh báo bạn "đừng nhận nhầm phân kênh thành multi-tenant". Với checkout đầy đủ, phát hiện
mới cho thấy **có một cơ chế multi-tenant thật, tách biệt hoàn toàn với phân kênh**:

```java
// commercial-common-lib — dùng chung ở gần như mọi service
TenantHikariDataSource   // mỗi connection JDBC lấy ra: SET app.partners = '<partner>'
MultiTenantFilter        // đọc tenant/partner từ HTTP header, đặt vào PartnerContext mỗi request
```

`TenantHikariDataSource` đặt session variable Postgres `app.partners` trên **từng connection**
lấy từ pool — đây chính xác là kỹ thuật chuẩn để nuôi **Row-Level Security** (RLS policy đọc
`current_setting('app.partners')` để lọc dòng). Kết hợp với các Flyway migration RLS đã thấy ở
bản 1 (`V5_4__update_user_account_rls_policy_.sql` trong `commercial-svc-auth`,
`commercial-svc-customer`), đây rất có khả năng là **cùng một cơ chế tenant isolation xuyên toàn
hệ thống** — dù chưa grep được câu SQL `CREATE POLICY` nối trực tiếp hai đầu (xem §0).

**Phân biệt rõ hai khái niệm — đây là chỗ dễ nói sai nhất khi phỏng vấn:**

| | `SystemType` (SHARED/CONSUMER/CONSUMERT1/BUSINESS/RLOC) | `PartnerContext` / `app.partners` |
|---|---|---|
| Là gì | **Phân kênh/product line** — routing tới NextGen vs Commercial vs legacy core T1 | **Tenant isolation thật** — mỗi partner/khách hàng doanh nghiệp một bộ dữ liệu tách biệt bằng RLS |
| Ở đâu | `GatewayProperties`, `CrossGatewayController` — tầng gateway | `commercial-common-lib` — tầng connection pool, dùng ở mọi service |
| Đối chiếu JD Katalon | Không liên quan trực tiếp | **Chính xác** câu JD hỏi: *"tenant isolation: row-level vs schema vs DB"* |

### 1.5 Workflow — xác nhận: thư viện đánh giá quy tắc, KHÔNG phải BPMN engine

`acb-workflow-lib` (giờ 83 file thật) xác nhận đúng hướng bản 1 đoán, với độ chi tiết cao hơn:

- **Không phải engine** — không có Camunda/Activiti/Temporal, không entity JPA nào trong lib này.
- Là một **`@Aspect`** (`WorkflowAspect`) bọc quanh method đánh dấu `@WorkflowAPI`: mỗi lần gọi,
  nó tải `Workflow` hiện tại, chạy **biểu thức SpEL** lấy từ `WorkflowRuleProperties`
  (`acb.workflow.rules.<partner>.<step>.<nextStep>: <SpEL>` — chính là nội dung
  `application-workflow.yml` bản 1 đã thấy) để quyết định bước kế tiếp.
- `WorkflowClient` là một Feign client (`workflow-service`, `http://localhost:9082`) — **service
  lưu trữ/orchestrate workflow thật nằm NGOÀI checkout này**, không có module nào tên
  `workflow-service` trong repo. Kết luận bản 1 "chưa thấy workflow-service" **vẫn đúng**.

### 1.6 Batch — ĐÍNH CHÍNH LỚN: Spring Batch + Quartz thật, không chỉ cron

Bản 1 kết luận "chỉ có `@Scheduled` cron" — **sai**, vì module này trước đó hoàn toàn rỗng.
`commercial-svc-batch` (725 file) là một **hệ thống batch production thật**, kết hợp hai
framework:

- **Spring Batch 3.0.7.RELEASE** (bản cũ, cấu hình XML `batch-jobs.xml` dài **1091 dòng**,
  `@EnableBatchProcessing` bị **comment-out** để tự import XML thủ công) — **50+ job/step**, có
  `RangePartitioner` cho partitioned step (chạy đa luồng thật, không phải vòng lặp đơn giản).
- **Quartz** với JDBC job store thật (bảng `QRTZ_*` tạo bằng Flyway), cron trigger **được seed
  qua migration** (ví dụ `0 0/30 * * * ?` mỗi 30 phút cho sync Salesforce, `0 0 11 * * ?` 11h sáng
  cho renewal RLOC). Một Quartz `Job` tuỳ biến (`JobLauncherDetails`) nhận trigger rồi **gọi
  `JobService.launch(...)` của Spring Batch** — Quartz chỉ làm lịch, Spring Batch làm việc thật.

**Nghiệp vụ thật xử lý ở đây** (suy ra từ tên class/job): tính lãi hàng ngày/tháng cho loan,
savings, RLOC; sinh/gửi/nhận file ACH; báo cáo GL và sao kê hàng ngày; báo cáo credit bureau
(**Metro2, e-Oscar, TransUnion, Experian**); đối soát (reconciliation); đồng bộ CRM (Salesforce,
iContact, Trellis); đồng bộ customer/entity sang hệ core cũ (T1).

> **Đây là kinh nghiệm thật rất gần với JD Katalon phần *"large-scale data processing, batch and
> real-time"*** — không phải Spark, nhưng là batch processing production thật ở quy mô tài chính,
> có partition, có scheduler JDBC-backed. Dùng làm câu trả lời cầu nối khi chưa có Spark thật.

### 1.7 Auth — Authorization Server thật, nhưng validate JWT vẫn phân tán

`commercial-svc-auth` (260 file, trước đó rỗng) xác nhận là service issue token **thật**, không
phải giả thuyết:

- Dùng **`spring-security-oauth2` (thư viện cũ, đã deprecated)** — `@EnableAuthorizationServer`,
  KHÔNG phải Spring Authorization Server hiện đại.
- Endpoint `/oauth/token` thật, hỗ trợ `password` grant (có 2FA cho `client_id=customer-portal`),
  `refresh_token`, và `client_credentials` (dùng cho gọi service-to-service). Client credentials
  lưu trong Postgres qua `OAuth2ClientDetailsService`.
- User lưu ở bảng `user_account` (Postgres, **có RLS policy** — liên hệ §1.4), password hash bằng
  **BCrypt** (có cache 29 phút).
- **Validate JWT vẫn phân tán:** endpoint `/oauth/check_token` tồn tại nhưng **không tìm thấy nơi
  nào gọi tới nó trong toàn repo**. Mỗi service tự verify chữ ký JWT bằng **file khoá PEM/JKS
  chia sẻ sẵn** (đặt cứng trong `src/main/resources/keys/`), không có JWKS endpoint sống. Đây là
  kiểu phân phối khoá **thủ công**, không theo chuẩn OAuth2 hiện đại (JWKS rotation).
- Gateway **không gọi** `commercial-svc-auth` để xác thực — `SecurityConfig` của gateway
  `permitAll()` với comment thẳng *"Security will be handled by downstream services"*.

### 1.8 API Gateway — ĐÍNH CHÍNH LỚN: gateway THẬT, nhưng thiếu cross-cutting concern chuẩn

Đây là sửa quan trọng nhất của cả bản 2. `commercial-svc-gateway` (123 file, trước đó rỗng) là
một **Spring Cloud Gateway (reactive, WebFlux) đang chạy thật** — không phải "quy hoạch nhưng
chưa xây" như bản 1 kết luận.

**Có bằng chứng dùng thật:**
- Route định nghĩa `spring.cloud.gateway.routes` theo profile — path predicate + `StripPrefix`.
- **`CrossGatewayController`** — pattern **scatter-gather** thật: fan-out một request tới nhiều
  backend theo `SystemType` (Consumer/Business/Agent) cùng lúc, gộp kết quả có gắn tag hệ thống
  nguồn. Đây là một distributed-system pattern đáng nói khi phỏng vấn.
- `ForceLogoutFilter` — check Redis xem username có bị force-logout không, trả 401 nếu có — logic
  session-kill tập trung **thật**.
- `GatewayCorsConfig`/`CORSConfig` — CORS thật, cấu hình theo profile.
- `ExecutionTimeFilter` — thêm header thời gian xử lý.
- `NgCoreHealthCheckStrategy` (và tương tự cho ng-account/ng-customer/ng-origination) — gateway
  **là điểm hợp nhất thật cho cả hai dòng NextGen và Commercial**, không phải hai gateway riêng.

**Vẫn thiếu — và đây mới là hệ quả thật đáng nói:**
- **Không validate JWT tại gateway** — `AuthenticationFilter` chỉ promote cookie `ACCESSTOKEN`
  thành header `Authorization`, không verify chữ ký. Tin tưởng hoàn toàn downstream.
- **Không rate limiting** — không `RequestRateLimiter`, không Resilience4j, không Bucket4j.
- **Không circuit breaker ở gateway** — Hystrix vẫn chỉ per-service như bản 1 đã thấy.
- **Không service discovery** — Eureka tắt hoàn toàn, route bằng URL tĩnh.
- Có dấu hiệu rõ của **một cuộc di trú Zuul → Spring Cloud Gateway chưa dọn sạch**: cơ chế
  refresh Swagger (`ServiceDescriptionUpdater`) vẫn tham chiếu property `zuul.routes.*` không còn
  tồn tại ở đâu cả — code chết từ thời Zuul, chưa bị xoá khi chuyển sang Spring Cloud Gateway.

### 1.8b ĐÍNH CHÍNH LỚN: ALB + autoscaling ĐÃ XÁC NHẬN bằng Terraform thật ở repo infra riêng

> **Lịch sử của mục này:** ban đầu tôi chỉ có app-repo `all-in-one-v2`, không tìm thấy
> Terraform/CloudFormation nào, nên chỉ đưa ra được một **suy luận cấu trúc** — "phải có ALB, không
> thì route tĩnh không giải thích được việc scale". Sau đó tìm ra hạ tầng thật nằm ở một **repo Git
> riêng** (`fe-devops/commercial-infras`, xem callout Bản 3 đầu file). Suy luận đó giờ **đúng, và có
> bằng chứng file:line** — giữ lại cấu trúc lập luận cũ vì nó vẫn là cách đúng để *nghĩ ra* câu trả
> lời khi không có Terraform trong tay, nhưng thay phần "chưa xác nhận" bằng HCL thật.

Câu hỏi hay bị hỏi: *"nếu 1 service bị cao tải thì scale lên thế nào, Gateway hỗ trợ gì để connect
tới các instance đã scale, và ALB đóng vai trò gì?"* — cần tách rõ **ba lớp**.

#### Lớp 1 — CI/CD app-repo chỉ rolling-deploy; autoscaling THẬT nằm ở infra-repo, StepScaling theo CloudWatch alarm

`codebuild/buildspec.yml` trong **app-repo** `all-in-one-v2` chỉ làm 2 lệnh AWS CLI, không hơn
(`commercial-svc-gateway/codebuild/buildspec.yml:41,50`):

```bash
TASK_DEFINITION_ARN=$(aws ecs register-task-definition --cli-input-json file://ecs-${ECS_ENV}-${MICROSERVICE}.template | jq '.taskDefinition.taskDefinitionArn')
aws ecs update-service --cluster "acb-${ECS_ENV}" --service "${MICROSERVICE}" \
    --task-definition "$TASK_DEFINITION_ARN" \
    --deployment-configuration "maximumPercent=...,minimumHealthyPercent=..."
```

Đây đúng là **rolling update lên một revision task-definition mới**, không phải lệnh scale — không
có `--desired-count` nào được truyền, và app-repo không có CloudFormation/Terraform nào cả. **Nhưng
đó chỉ là nửa câu chuyện** — autoscaling thật nằm ở **infra-repo riêng**
(`fe-devops/commercial-infras/acb/acb-cluster/business/ECS.tf` + `autoScale.tf`):

```hcl
# business/ECS.tf:81-96 — aws_ecs_service, desired_count đọc từ tfvars
resource "aws_ecs_service" "this" {
  for_each        = var.service_conf
  desired_count   = each.value.desired_count   # production.tfvars: 2 (account/core/gateway/...), 1 (batch)
  launch_type     = "FARGATE"
  dynamic "load_balancer" { ... target_group_arn = aws_lb_target_group.this[...] ... }
}

# business/ECS.tf:124-217 — StepScaling, KHÔNG phải TargetTracking
resource "aws_appautoscaling_target" "scale_target" {
  scalable_dimension = "ecs:service:DesiredCount"
  max_capacity       = var.ecs_autoscale_max_instances   # production.tfvars = 2
  min_capacity       = var.ecs_autoscale_min_instances   # production.tfvars = 1
}
resource "aws_appautoscaling_policy" "scale-out-cpu" {
  step_scaling_policy_configuration {
    adjustment_type = "ChangeInCapacity"
    cooldown        = 60
    step_adjustment { metric_interval_lower_bound = 0, scaling_adjustment = 1 }
  }
}
# scale-in-cpu (cooldown 300), scale-out-memory, scale-in-memory — cùng shape StepScaling
```

Kích hoạt bằng **CloudWatch alarm** trên metric `AWS/ECS` (`business/autoScale.tf:65-101`):

```hcl
resource "aws_cloudwatch_metric_alarm" "cpu_utilization_high_autoscale" {
  metric_name   = "CPUUtilization"
  namespace     = "AWS/ECS"
  alarm_actions = [aws_appautoscaling_policy.scale-out-cpu[each.key].arn]
}
```

> **Vì sao StepScaling chứ không phải TargetTracking, và đây là chi tiết đáng nói khi phỏng vấn:**
> TargetTracking (ví dụ giữ CPU ở 60%) tự tính số bước cần tăng dựa trên *khoảng cách* tới target —
> phản ứng mượt và tỉ lệ với mức độ lệch. StepScaling chỉ biết "vượt ngưỡng → +1 task, cooldown 60s"
> — thô hơn, phản ứng theo bước cố định bất kể vượt ngưỡng bao nhiêu, nhưng đơn giản hơn để audit
> (dev đọc alarm + policy là hiểu ngay ngưỡng nào gây ra hành động nào). Đây là kiểu lựa chọn "dễ
> vận hành thủ công hơn là tối ưu tự động" — hợp lý với `max_capacity=2` rất nhỏ (không có nhiều
> không gian để TargetTracking phát huy).

**Một nơi autoscaling bị TẮT — đáng nói vì cho thấy các product line không đồng đều:**
`rloc/ECS.tf:124-158` có `aws_appautoscaling_target` (target tồn tại) nhưng **toàn bộ
`aws_appautoscaling_policy` bị comment**:

```hcl
resource "aws_appautoscaling_target" "scale_target" { ... }   # vẫn active
# resource "aws_appautoscaling_policy" "scale-out" { ... }    # ← BỊ COMMENT, dormant
# resource "aws_appautoscaling_policy" "scale-in"  { ... }
```

→ `rloc` (product line cũ hơn, commit gần nhất 2022) có khung autoscaling nhưng **không thực sự tự
scale** — phải tăng `desired_count` bằng tay nếu cao tải. `business`/`shared` (dòng đang phát triển,
commit tới 05-06/2025) thì autoscaling **đang hoạt động thật**.

#### Lớp 2 — Spring Cloud Gateway: route TĨNH, tắt hẳn service discovery

Đây là chỗ cần phân biệt **Spring Cloud Gateway hỗ trợ được gì về nguyên tắc** với **cái repo này
thật sự dùng**.

**Về nguyên tắc, Spring Cloud Gateway hỗ trợ 2 kiểu route:**

| Kiểu URI | Cách hoạt động | Cần gì đi kèm |
|---|---|---|
| `http://host:port` (static) | Trỏ cứng một địa chỉ — Gateway không biết và không cần biết phía sau có bao nhiêu instance | Không cần gì thêm, nhưng **không tự scale-aware** |
| `lb://service-id` | Gateway tra `DiscoveryClient` (Eureka/Consul/Nacos...) ra danh sách instance, rồi `spring-cloud-starter-loadbalancer` tự chọn 1 instance theo round-robin/random — **client-side load balancing thật** | Cần service registry đang chạy + service tự đăng ký |

**Repo này dùng đúng kiểu thứ nhất, và tắt hẳn kiểu thứ hai** — bằng chứng ở cả 13 file route quan
sát được (`application-docker.yml:34`, `application-local.yml`, `application-lb-buss-local.yml`,
`application-localhost-cons.yml`...), toàn bộ URI là literal, ví dụ:

```yaml
uri: http://localhost:8089          # auth-service
uri: http://host.docker.internal:8089
uri: http://localhost:5086          # origination-service (LOCAL profile — xem đính chính dưới)
```

> ⚠️ **Tự đính chính port số cụ thể:** con số `5081`/`5086`/`9082` tôi trích ở trên đến từ
> `application-lb-buss-local.yml` — một profile **chỉ dùng để chạy nhiều service cùng lúc trên 1
> máy dev** (offset port để tránh đụng nhau), **không đại diện cho production**. Đối chiếu với
> `business/tfvars/production.tfvars` (infra-repo thật) thì container port production là
> `batch:8081, core:8084, origination:8086, communication:8083, account:8085, reporting:8088` — và
> con số này khớp gần như chính xác với một profile local **khác** (`application-lb-cons-local.yml`),
> không phải profile tôi trích ban đầu. Bài học: **có nhiều profile local song song trong cùng
> repo, dễ trích nhầm profile khi không đối chiếu với infra thật.**

Và service discovery bị tắt tường minh ở **mọi nơi nó được nhắc tới** — không phải quên cấu hình,
mà là tắt có chủ đích:

```yaml
# commercial-svc-gateway/application.yml:60,64 (và lặp lại ở account/core/auth/reporting/...)
spring:
  cloud:
    loadbalancer:
      ribbon:
        enable: false
ribbon:
  eureka:
    enabled: false
```

Repo-wide grep `lb://` → **0 kết quả**. Không một route nào dùng scheme này.

> ⚠️ **Bẫy dễ bị đánh lừa:** `pom.xml:105` của `commercial-svc-gateway` **có** khai báo dependency
> `spring-cloud-starter-loadbalancer` — nhìn vào `pom.xml` không thì tưởng có load balancing thật.
> Nhưng có dependency ≠ có tính năng đang chạy: `ribbon.enable: false` tắt đường tích hợp với
> registry, và không route nào dùng `lb://` để kích hoạt nó. Đây cùng loại "tín hiệu giả" như
> `zuul.routes.*` chết đã ghi ở mục 1.8 — thư viện còn trong classpath từ một giai đoạn thiết kế
> trước, chưa dọn, không phản ánh hành vi runtime thật.

**Hệ quả trực tiếp cho câu hỏi scale:** nếu service `account-service` được scale từ 1 lên N task,
**Gateway hoàn toàn không biết** — nó vẫn gửi mọi request tới đúng một địa chỉ cấu hình sẵn. Gateway
không tự phân tải giữa N task đó, vì nó không có danh sách N task đó ở đâu cả.

> ⚠️ **Đính chính thêm — "địa chỉ cấu hình sẵn" trong production KHÔNG nằm trong YAML.** File route
> mẫu ở trên (`application-local.yml`...) chỉ dùng cho dev/docker. Production/staging thật load
> route qua `spring.config.import: aws-secretsmanager:/buss/prod/gateway` (`application-profile.yml`)
> — giá trị URI thật được inject từ **AWS Secrets Manager** lúc container start, theo môi trường.
> Điều này **không** đổi kết luận "không có `lb://`, không service discovery" — cơ chế vẫn là trỏ
> tới một địa chỉ cố định theo cấu hình — nhưng địa chỉ đó được nạp động theo profile, không hard-code
> trong source. Tôi chưa grep được giá trị secret thật (không có trong infra-repo, chỉ còn ở
> `archived/secrets-archived/`), nên chuỗi URI chính xác Gateway trỏ tới trong production vẫn là
> suy luận từ quy ước tên (xem Lớp 3).

#### Lớp 3 — ALB: giờ ĐÃ XÁC NHẬN bằng Terraform thật, không còn là suy luận

`business/ALB.tf:50-73` (infra-repo riêng) — một `aws_lb` cho mỗi service, driven bởi
`local.alb_config`:

```hcl
resource "aws_lb" "this" {
  for_each                   = { for alb in local.alb_config : "..." => alb if !alb.shared_alb }
  load_balancer_type         = "application"
  internal                   = each.value.internal
  subnets                    = each.value.internal ? private_subnets : public_subnets
  enable_deletion_protection = true
}
```

Target group + health check thật (`business/ALB.tf:131-156`):

```hcl
resource "aws_lb_target_group" "this" {
  port        = 80
  target_type = "ip"          # đúng kiểu ECS Fargate awsvpc cần — target theo IP, không theo instance
  health_check {
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = each.value.service == "batch" ? "/info" : "/health"
    timeout             = 6
    unhealthy_threshold = 2
  }
}
```

Và **DNS ổn định** mà Gateway/Secrets-Manager-route có thể trỏ tới thật sự tồn tại — private hosted
zone Route53 với A-alias record trỏ tới ALB (`business/route53.tf:1-29`):

```hcl
resource "aws_route53_zone" "private" {
  name = var.route53_private_dns   # "prod-business"
  vpc { vpc_id = data.terraform_remote_state.this.outputs.vpc_id }
}
resource "aws_route53_record" "this" {
  for_each = { for alb in local.alb_config : "..." => alb if !alb.shared_alb }
  name     = each.value.service    # "core", "batch", "origination"... — TRÙNG tên key service_conf
  type     = "A"
  alias { name = aws_lb.this[each.key].dns_name, evaluate_target_health = true }
}
```

Nói cách khác: **việc load balancing giữa các replica không xảy ra ở Gateway — nó xảy ra ở ALB, và
Gateway hoàn toàn không biết chuyện đó đang diễn ra.** Đây không còn là "cách giải thích duy nhất
hợp lý" — nó là kiến trúc thật, đọc trực tiếp từ HCL.

**Chi tiết kiến trúc đáng nói thêm — "một cửa công khai duy nhất":** `gateway` được cấu hình
`internal = false` (`business/tfvars/production.tfvars`), nên ALB của nó nằm ở **public subnet**
— là cửa duy nhất mở ra internet. Mọi service khác (`auth`, `core`, `account`, `communication`,
`origination`, `reporting`, `customer`, `audit`, `credit`...) đều `internal = true` — ALB private
subnet, chỉ gọi được từ trong VPC. **Ngoại lệ đáng nói:** `batch` cũng được set `internal = false`
— có ALB public riêng, song song với Gateway, không đi qua "một cửa" — một điểm lệch khỏi pattern
"gateway là cửa duy nhất" mà nên nêu nếu bị hỏi sâu.

```text
[Gateway]  --https://core.prod-business (Route53 private zone, alias→ALB DNS)-->  [ALB internal]
                (route nạp từ Secrets Manager, 1 địa chỉ ổn định, không đổi khi scale)    │
                                                                                    Target Group
                                                                              (health_check: /health,
                                                                               interval 30s, matcher 200)
                                                                              ┌──────┬──────┬──────┐
                                                                            [Task 1][Task 2][Task N]
                                                                          (StepScaling theo CloudWatch
                                                                           alarm CPU/Memory AWS/ECS)
```

**Vai trò cụ thể ALB đảm nhiệm — mà Gateway không làm:**

- **Target Group + health check**: ECS đăng ký/hủy đăng ký task vào Target Group khi task
  start/stop; ALB chỉ gửi traffic tới task đã pass health check.
- **Load balancing giữa replica**: round-robin/least-outstanding-requests giữa N task — đúng chỗ mà
  Gateway (do route tĩnh, không `lb://`) không làm được.
- **Ổn định địa chỉ khi scale**: task IP đổi liên tục (Fargate `awsvpc`), nhưng DNS của ALB **không
  đổi** — đây là lý do Gateway "route tĩnh" vẫn hoạt động được dù service phía sau scale lên/xuống.
- **TLS termination, connection draining khi task bị rút khỏi target group lúc deploy/scale-in.**

**Bảng phân vai — ai làm gì khi 1 service bị cao tải (đã cập nhật, có file:line thật):**

| Việc cần làm | Ai làm | Bằng chứng |
|---|---|---|
| Tăng số task (scale-out) | `aws_appautoscaling_policy` (StepScaling) trên `business`/`shared`; **thủ công** trên `rloc` (policy bị comment) | ✅ `business/ECS.tf:124-217`, `autoScale.tf:65-101` |
| Thêm/xoá target khi task start/stop | ALB Target Group (`target_type = "ip"` — đúng kiểu Fargate `awsvpc`) | ✅ `business/ALB.tf:131-156` |
| Phân tải request giữa các task | ALB (round-robin, health check `/health` hoặc `/info` cho batch) | ✅ `business/ALB.tf:143-156` |
| Địa chỉ ổn định cho Gateway trỏ tới dù task IP đổi | Route53 private zone, A-alias → ALB DNS | ✅ `business/route53.tf:1-29` |
| Route theo path/predicate, cross-cutting (CORS, force-logout, scatter-gather) | **Spring Cloud Gateway** | ✅ Bằng chứng thật — mục 1.8 |
| Chịu lỗi khi 1 task chết giữa request | ALB (health check) — **KHÔNG phải Gateway** (không circuit breaker ở gateway, mục 1.8) | ✅ (thiếu circuit breaker ở Gateway) + ✅ (ALB health check thật) |
| Giá trị route Gateway trỏ tới trong production | AWS Secrets Manager (`aws-secretsmanager:/buss/prod/gateway`) | 🟡 Cơ chế xác nhận, **giá trị secret cụ thể chưa grep được** |

> **Câu trả lời gọn khi bị hỏi trực tiếp — và đây là câu nên thuộc:**
>
> *"Route ở Spring Cloud Gateway là cấu hình tĩnh (nạp từ Secrets Manager theo môi trường lúc
> start) và service discovery bị tắt hẳn — Eureka disabled, Ribbon disabled, không route nào dùng
> `lb://`. Gateway **không** tự làm load balancing giữa các instance đã scale. Việc đó xảy ra ở
> ALB: mỗi service có Target Group riêng với health check, ECS tự đăng ký/hủy task khi scale, và
> một private Route53 zone đặt A-alias record theo đúng tên service trỏ tới ALB DNS — đó chính là
> địa chỉ ổn định mà route tĩnh của Gateway trỏ tới. Autoscaling dùng StepScaling theo CloudWatch
> alarm CPU/Memory, cooldown 60s khi scale-out và 300s khi scale-in — chọn StepScaling chứ không
> TargetTracking vì hệ thống vận hành ở quy mô nhỏ (`max_capacity=2`), đơn giản để audit hơn là tối
> ưu tự động mượt."*

> ⚠️ **Ranh giới trung thực còn lại — chỉ một chỗ vẫn là suy luận, không phải cả kết luận:** kiến
> trúc ALB/Target Group/Route53/autoscaling ở trên **đã xác nhận bằng HCL thật**, không còn là suy
> luận. Phần **vẫn chưa xác nhận trực tiếp** là: chuỗi URI/host cụ thể mà secret
> `/buss/prod/gateway` chứa — tôi suy luận nó trỏ tới đúng những Route53 record này dựa vào quy ước
> tên trùng khớp (`service_conf` key = `route53` record name), nhưng chưa đọc được giá trị secret
> thật. Nói khi bị hỏi: *"Kiến trúc hạ tầng tôi xác nhận được bằng Terraform. Giá trị secret cụ thể
> Gateway đọc lúc runtime thì không nằm trong repo tôi có — tôi suy luận nó khớp quy ước tên, mức độ
> tin cậy cao nhưng chưa phải đọc trực tiếp."*

### 1.9 AWS — S3 thật, KMS mới, ECS xác nhận rộng hơn

- **S3 thật** (bản 1 không tìm thấy): `commercial-svc-core/S3Service.java` dùng
  `AmazonS3Client` (**SDK v1**, không phải v2) — dùng cho lưu document, xử lý file trả về ACH,
  presigned URL, cert Cybersource. `commercial-svc-batch` **không tự gọi S3** — nó gọi Feign sang
  `commercial-svc-core` để lấy file, giao việc I/O thật cho service khác.
- **KMS mới, và dùng SDK v2 hiện đại** — module `acb-column-encryption-sdk` (mã hoá cột dữ liệu)
  dùng `software.amazon.awssdk:kms` + `secretsmanager` (v2), khác hẳn phần còn lại của repo vẫn
  dùng SDK v1. Đây là tín hiệu **hai thế hệ SDK cùng tồn tại** — đáng nói khi được hỏi về nợ kỹ
  thuật/migration.
- **Trong app-repo vẫn không có:** SQS, SNS, DynamoDB, Lambda, Step Functions — app code (Java)
  không tự gọi các service này. Email/SMS dùng **SendGrid/Twilio** (bên thứ ba), không dùng
  SES/SNS trực tiếp từ code nghiệp vụ.
- **ECS xác nhận rộng hơn nhiều:** `codebuild/{buildspec.yml,ecs-base.template}` có ở **8/11**
  module dòng Commercial (thiếu ở `acb-eoscar-sdk`, `acb-payment`, `commercial-svc-audit`) và ở
  `commercial-svc-customer`.

### 1.9b Bổ sung AWS sau khi review repo infra riêng — Lambda/SNS/Aurora/Redshift/WAF thật, EKS thật nhưng đã ngừng dùng

> ⚠️ **Đính chính trực tiếp dòng trên:** "không có SQS/SNS/DynamoDB/Lambda" chỉ đúng cho **app-repo**
> — tức đúng là code Java nghiệp vụ không tự gọi các service đó. Nhưng ở tầng **hạ tầng** (repo
> `fe-devops/commercial-infras` riêng), Lambda + SNS **có thật**, và không phải nghiệp vụ mà là
> **vận hành/observability** — khác vai trò với SQS/DynamoDB (dữ liệu nghiệp vụ, vẫn không thấy ở
> đâu cả). Tương tự, "không có Kubernetes/Helm" chỉ đúng cho app-repo.

**Lambda + SNS + CloudWatch Events — thật, dùng cho vận hành (không phải business logic):**

- `acb/lambda/elb-access-log-parser.tf` — Lambda trigger bởi `aws_s3_bucket_notification`
  (`s3:ObjectCreated:*`), đẩy ALB access log vào Elasticsearch (`ELASTICSEARCH_URL`).
- `acb/lambda/endpoint-checker.tf` — Lambda chạy theo `rate(1 minute)` CloudWatch Events, báo lỗi
  qua **SNS topic thật** `arn:aws:sns:us-east-1:796554719752:csc-slack-notification`.
- `acb/lambda/sns2msteam.tf` — Lambda forward thông báo SNS sang Microsoft Teams webhook.
- → Đây là **health-check/alerting layer**, không phải nơi domain event (order/payment) chảy qua —
  đừng lẫn với kiểu "event-driven business logic qua SNS/SQS" khi trả lời.

**Aurora PostgreSQL — thật, qua module chính thức, không phải RDS instance đơn:**
`business/rds.tf`, `consumer/rds.tf`, `shared/rds.tf`, `rloc/rds.tf` đều gọi
`terraform-aws-modules/rds-aurora/aws` v5.3.0, `engine = "aurora-postgresql"` — sửa liên tục tới
2024-2025 (`shared/rds.tf` commit 2024-08-13 "Add new certificate for shared"). Khớp với việc
Postgres được xác nhận là DB chính ở mục 1.3.

**Redshift — thật, cho data warehouse, hoàn toàn tách biệt:**
`commercial-infras/datawarehouse/redshift.tf` — `aws_redshift_cluster` riêng, backend Terraform
riêng (`bucket="commercial-devops-tfstate"`, khác `acb-devops-tf` của dòng ACB đang sống) — một
nhánh warehouse cũ hơn, không liên quan trực tiếp tới OLTP path.

**WAF gắn ALB — một cái thật, một cái bị comment hết (chưa live):**

```hcl
# waf/batch.tf:48-55 — THẬT
resource "aws_wafv2_web_acl_association" "batch" {
  resource_arn = data.aws_lb.batch.arn   # ALB thật: "csc-prod-batch"
  web_acl_arn  = aws_wafv2_web_acl.batch.arn
}
```

```hcl
# waf/gateway.tf — TOÀN BỘ FILE bị comment, KHÔNG live
# resource "aws_wafv2_web_acl_association" "gateway" { ... }
```

→ Batch (có ALB public riêng, xem 1.8b) được WAF bảo vệ thật. Gateway — cửa công khai chính — **có
WAF được định nghĩa sẵn trong code nhưng chưa bật**. Đáng nói nếu bị hỏi "hệ thống có WAF không":
câu trả lời chính xác là *"có, nhưng không đồng đều — service chính (gateway) chưa được bật, một
service phụ (batch) thì có"*, không phải "có" hay "không" đơn giản.

**EKS — một cụm thật, nhưng đã ngừng cập nhật từ 08/2023, bị thay bằng ECS Fargate:**
`devops-cluster/k8s/eks-cluster.tf` — `module "eks"` (`terraform-aws-modules/eks/aws` v12.2.0,
`cluster_version = "1.20"`, worker group `r5.xlarge`, `fargate-profile.tf`) — cụm tên `csc-devops`,
chạy ELK/Jenkins-on-K8s/SonarQube/monitoring/ActiveMQ. Commit cuối chạm tới thư mục này:
`35d46205 "update prometheus cm"`, **10/08/2023** — không có commit nào sau đó. Trong khi đó,
Jenkins cho dòng ACB hiện tại chạy trên **ECS Fargate** (`acb/acb-cluster/devops/jenkins.tf`, EFS
volume cho `/var/jenkins_home`, gắn ALB) — tín hiệu rõ của một cuộc **di trú khỏi EKS sang ECS
Fargate** cho tooling nội bộ, để lại cụm EKS cũ chưa xoá nhưng không còn cập nhật.

> **Câu trả lời gọn khi bị hỏi "hệ thống có dùng Kubernetes không":**
>
> *"Có, nhưng là chuyện lịch sử. Có một cụm EKS thật tên `csc-devops` chạy tooling nội bộ — ELK,
> Jenkins, SonarQube, monitoring — nhưng Terraform của nó ngừng được cập nhật từ tháng 8/2023.
> Song song đó, Jenkins cho dòng sản phẩm đang phát triển hiện tại lại chạy trên ECS Fargate, có
> commit tới giữa 2025. Đọc hai tín hiệu đó cùng nhau, tôi suy luận đây là một cuộc di trú khỏi K8s
> sang ECS đã hoàn tất cho phần tooling, chỉ còn để lại code cũ chưa dọn — không phải K8s đang được
> dùng cho service nghiệp vụ nào."*

### 1.10 Version stack — không đổi

Java **11** · Spring Boot **2.7.18** · Spring Cloud **2021.0.9** — nhất quán trên toàn bộ module
mới lẫn cũ. Spring Batch 3.0.7.RELEASE (cũ) + Quartz (JDBC store). Điểm ngoại lệ duy nhất: SDK AWS
v2 ở `acb-column-encryption-sdk`.

---

## 2. So sánh với system design Katalon (đã cập nhật)

| Khía cạnh | all-in-one-v2 (thật, bản 2) | Katalon (theo JD) | Ghi chú |
|---|---|---|---|
| Ngôn ngữ/framework | Java 11, Spring Boot 2.7.18 | Java 21, Quarkus/Spring Boot 3.3 | Cần ôn — [module 02](../katalon-prep-java/02-quarkus-service/) |
| Giao tiếp đồng bộ | Feign, cả xuyên 2 product line | Feign/REST tương tự | ✅ Khớp |
| Giao tiếp bất đồng bộ | RabbitMQ; Kafka **cấu hình chết**, chưa từng chạy | **Kafka** | Khái niệm giống, API khác — [module 06](../katalon-prep-java/06-distributed-resilience/) |
| **Tenant isolation** | **RLS thật qua `PartnerContext`/`app.partners`**, tách biệt khỏi phân kênh | JD hỏi đúng: *"row-level vs schema vs DB"* | ✅ **Bằng chứng mạnh nhất bạn có — xem §1.4/§4** |
| **API Gateway** | **Thật** (Spring Cloud Gateway), nhưng thiếu auth/rate-limit/circuit-breaker tập trung | Cần gateway đủ chức năng cho SaaS | Câu chuyện "gateway có nhưng thiếu nửa" — mạnh hơn "chưa có" |
| Auth | Authorization Server thật (OAuth2 cũ) + validate phân tán qua key file tĩnh | Cần *"secure, observable"* | RCI (CV) đã là bản hiện đại — dùng làm đối chiếu |
| Circuit breaker | Hystrix per-service, không ở gateway | Cần circuit breaker 3 pha chuẩn | [module 08](../katalon-prep-java/08-system-design/) |
| Batch processing | **Spring Batch 3.0.7 + Quartz thật**, 50+ job production | *"large-scale data processing, batch and real-time"* → Spark/PySpark | Không phải Spark, nhưng là batch thật ở quy mô tài chính — dùng làm cầu nối |
| Container orchestration | AWS ECS (đa số service Commercial) | Docker + **Kubernetes** (yêu cầu cứng) | 🔴 Gap thật — ECS ≠ K8s, xem mapping §3 |
| Object storage | S3 thật (SDK v1) | S3/tương đương | ✅ Có kinh nghiệm thật, dù SDK cũ |
| Secrets/encryption | Secrets Manager + KMS (SDK v2, module mới) | Thực hành chuẩn | ✅ Dùng được, và cho thấy bạn quen cả SDK v1 và v2 |
| AI/LLM | Không có | Cốt lõi — TrueTest | Dùng [Motives IDP](../CV-BASED-ANSWERS.md) cho phần này |

---

## 3. Ba hệ thống, một đường tiến hoá

```text
all-in-one-v2                     RCI                              Katalon (mục tiêu)
─────────────                     ───                              ──────────────────
Java 11 / Spring 2.7          →   Java 21 / Spring 3.3         →   Java 21 / Quarkus
OAuth2 cũ + key file tĩnh     →   Spring Authorization Server →   (xác nhận thêm ở JD)
Gateway thật, thiếu nửa       →   Có API Gateway (theo CV)     →   Cần gateway đủ chức năng
RabbitMQ (Kafka: chết)        →   RabbitMQ/Spring Cloud Stream →   Kafka
AWS ECS                       →   Docker (K8s: chưa xác nhận)  →   Docker + Kubernetes
RLS qua PartnerContext        →   (chưa rõ multi-tenant)       →   SaaS multi-tenant thật
Spring Batch + Quartz thật    →   (chưa rõ)                    →   Spark/PySpark
Không AI                      →   Không AI                     →   AI-augmented (TrueTest)
```

**Câu trả lời mẫu — giờ mạnh hơn bản 1 vì có bằng chứng tenant isolation thật:**

> "Tôi đã làm việc thật với tenant isolation ở mức Row-Level Security — một hạ tầng dùng chung
> đặt session variable Postgres theo partner trên mọi connection lấy từ pool. Tôi cũng đã thấy
> một API Gateway được xây thật nhưng thiếu nửa — có cross-cutting logic thật (session kill qua
> Redis, scatter-gather đa hệ thống) nhưng không centralize auth/rate-limit. Nên câu hỏi "làm sao
> thiết kế multi-tenant SaaS đúng" với tôi không phải lý thuyết — tôi biết chính xác phần nào dễ
> làm đúng và phần nào dễ bị bỏ sót."

---

## 4. Phân biệt chính xác: phân kênh vs tenant isolation — nói đúng, không nói lẫn

Khác với cảnh báo ở bản 1 ("đừng nhận nhầm multi-tenant"), giờ bạn **có quyền nhận** kinh nghiệm
multi-tenant thật — nhưng phải trỏ đúng cơ chế:

**Đừng nói:** *"SystemType (phân kênh Consumer/Business) chính là tenant isolation của tôi"* — sai,
đó là routing theo product line, không phải theo khách hàng.

**Nên nói:**

> "Tôi có kinh nghiệm thật với Row-Level Security cho tenant isolation — một lớp hạ tầng dùng
> chung (`TenantHikariDataSource`) đặt session variable Postgres theo partner trên **mọi**
> connection lấy ra từ pool, kết hợp với RLS policy ở tầng database. Một request tới đọc tenant từ
> HTTP header và set vào context ngay đầu vòng đời request. Tôi hiểu trade-off của row-level so
> với schema-per-tenant hay database-per-tenant: row-level chia sẻ schema nên tốn ít tài nguyên
> nhưng rủi ro nếu policy viết sai hoặc quên set session variable ở một code path nào đó; cách ly
> mạnh hơn (schema/DB riêng) an toàn hơn nhưng tốn kém và khó vận hành ở quy mô lớn."

---

## 5. Bộ câu hỏi phỏng vấn system design

### 5.1 Câu hỏi chung — Katalon nhiều khả năng hỏi (map theo JD)

1. Thiết kế một hệ thống test execution phân tán, multi-tenant, chịu được một tenant gửi 10.000
   test cùng lúc mà không ảnh hưởng tenant khác.
2. So sánh trade-off giữa row-level, schema-level, và database-level tenant isolation.
3. Thiết kế rate limiting cho API công khai có nhiều tier khách hàng.
4. Circuit breaker và retry — vì sao cần cả hai? Vẽ máy trạng thái 3 pha.
5. Thiết kế hệ thống thu thập event thời gian thực từ hàng nghìn browser session (bài toán
   TrueTest) — batch hay streaming?
6. Một consumer Kafka crash giữa lúc xử lý rồi restart — điều gì xảy ra? Đảm bảo idempotent thế nào?
7. Có 10.000 request/phút, mỗi request trả về true/false. Thiết kế ingestion và
   rollup để trả lời tỷ lệ theo từng tenant trong **2 giờ và 24 giờ gần nhất**, với
   số liệu real-time hoặc trễ vài giây: Kafka partition key, số thread/consumer,
   dedupe, late event, watermark, và cách tránh race condition khi ghi DB?
8. K8s: pod bị OOMKilled liên tục — quy trình debug?
9. Thiết kế pipeline AI đọc DOM khách hàng để sinh test case — lo gì về an toàn dữ liệu? *(→
   [common/04](../katalon-prep-common/04-ai-agent-system-design.md))*

### 5.2 Câu hỏi rèn từ chính all-in-one-v2 — dùng được câu chuyện thật (bản 2)

9. **"Bạn có kinh nghiệm tenant isolation thật không?"**
   → Trả lời bằng §4, KHÔNG dùng SystemType. Nói rõ cơ chế `TenantHikariDataSource` +
   `MultiTenantFilter` + RLS.

10. **"Bạn từng thấy một API Gateway được xây thật nhưng bỏ sót cross-cutting concern nào? Bạn sẽ
    sửa gì đầu tiên?"**
    → Gateway có logic session-kill qua Redis và scatter-gather thật, nhưng **không** validate
    JWT tập trung (chỉ promote cookie→header) và **không** rate limit. Sửa đầu tiên: thêm
    `GlobalFilter` verify chữ ký JWT ngay tại gateway (dùng lại key/JWKS thay vì tin downstream),
    vì đây là chỗ một request độc hại đi xa nhất mà không bị chặn.

11. **"Vì sao một hệ thống dùng CẢ Spring Batch VÀ Quartz cùng lúc? Có dư thừa không?"**
    → Không dư thừa nếu phân đúng vai trò: Quartz lo **lập lịch** (cron, JDBC-backed nên chịu
    được restart/cluster), Spring Batch lo **thực thi** (chunk processing, partition, retry theo
    step). Quartz gọi vào Spring Batch qua một `Job` tuỳ biến — tách lịch khỏi logic nghiệp vụ.
    Rủi ro thật: hai hệ thống độc lập nghĩa là hai nơi phải giám sát riêng khi job treo.

12. **"Hệ thống của bạn dùng cả AWS SDK v1 (S3) và v2 (KMS) cùng lúc. Đây có phải nợ kỹ thuật?
    Bạn nâng cấp thế nào mà không rủi ro?"**
    → Đúng, là nợ kỹ thuật thật — SDK v1 đã vào maintenance mode. Nâng cấp an toàn: viết adapter
    interface bọc quanh cả hai SDK, migrate module ít rủi ro nhất trước (ví dụ S3Service không có
    nhiều caller), giữ test đầy đủ trước khi đổi client thật.

13. **"Bạn nói dùng Hystrix — Hystrix ngừng phát triển từ 2018, bạn biết thay thế là gì?"**
    → Resilience4j, hoặc Spring Cloud Circuit Breaker abstraction. Câu bẫy kiểm tra cập nhật kiến
    thức.

14. **"Auth service của bạn issue token thật, nhưng không service nào gọi introspection endpoint.
    Rủi ro gì, và cách nào để có revocation thật (thu hồi token trước khi hết hạn)?"**
    → JWT tự-chứa (self-contained) nên không thể revoke giữa đường nếu chỉ verify chữ ký local —
    đây chính là lý do `ForceLogoutFilter` phải tồn tại riêng ở gateway (check Redis), vì token
    JWT bản thân không revoke được. Cách đúng hơn: token ngắn hạn + refresh token dài hạn +
    danh sách đen (blocklist) tập trung mà mọi service tra trước khi tin JWT.

15. **"Vì sao một hệ thống chọn schema-per-product-line (nextgen vs public) thay vì
    database-per-service chuẩn?"**
    → Hai product line (NextGen/Commercial) chia sẻ hạ tầng chung (`commercial-common-lib`,
    workflow-lib) nên việc share instance database giảm chi phí vận hành, còn tách schema vẫn cho
    phép migrate/quản lý độc lập ở mức schema. Đánh đổi: hai product line coupling ở tầng
    connection pool/instance, một sự cố hạ tầng ảnh hưởng cả hai.

16. **"Nếu một service bị cao tải, hệ thống của bạn scale lên thế nào? Gateway có tự biết route
    tới instance mới không?"**
    → Tách 3 lớp (chi tiết ở
    [mục 1.8b](#18b-đính-chính-lớn-alb--autoscaling-đã-xác-nhận-bằng-terraform-thật-ở-repo-infra-riêng)):
    route ở Spring Cloud Gateway là cấu hình tĩnh (nạp từ Secrets Manager theo môi trường lúc
    start), Eureka/Ribbon bị tắt tường minh, không route nào dùng `lb://` — Gateway **không** tự
    load balancing giữa các instance. Autoscaling thật (StepScaling theo CloudWatch alarm
    CPU/Memory, `min=1/max=2`) và ALB (Target Group + health check `/health`) đã **xác nhận bằng
    Terraform thật** ở một repo infra riêng (`fe-devops/commercial-infras`) — không còn là suy luận.
    Một Route53 private zone đặt A-alias record theo đúng tên service trỏ tới ALB DNS, đó chính là
    địa chỉ ổn định route tĩnh của Gateway trỏ tới dù task IP đổi khi scale.

17. **"Kể một lần bạn review/audit sai và phát hiện ra sao?"** — câu này bạn có sẵn câu chuyện
    **thật của chính quá trình chuẩn bị tài liệu này**, không phải bịa:
    → Lần đầu chỉ review app-repo `all-in-one-v2`, không tìm thấy Terraform/CloudFormation nào, nên
    kết luận "ALB có thể có nhưng chưa xác nhận" ở mức **suy luận cấu trúc**. Sau đó phát hiện hạ
    tầng thật nằm ở một **repo Git hoàn toàn riêng** (`fe-devops/commercial-infras`) — review nó thì
    xác nhận đúng suy luận cũ (ALB thật, autoscaling thật), nhưng cũng lật lại vài kết luận khác
    ("không có Lambda/SNS" → sai, chỉ đúng cho app-repo). Bài học: *"không tìm thấy X trong repo tôi
    đang xem"* và *"X không tồn tại trong hệ thống"* là hai câu khác nhau — nhầm lẫn ranh giới
    app-repo/infra-repo là lỗi review thật tôi đã mắc, không phải ví dụ lý thuyết.

### 5.3 Câu hỏi bẫy / follow-up sâu

16. *"Zuul hay Spring Cloud Gateway?"* → Repo có dấu vết cả hai (property `zuul.routes.*` chết
    trong code hiện tại dùng Spring Cloud Gateway) — dùng làm ví dụ nợ kỹ thuật từ một cuộc di
    trú chưa dọn sạch, không phải câu hỏi lý thuyết đơn thuần.
17. *"Gateway promote cookie thành Authorization header mà không verify — an toàn không?"* →
    Không đủ an toàn một mình: nó giả định cookie không giả mạo được (cần `HttpOnly`+`Secure`+
    domain đúng) và giả định downstream luôn verify đúng. Một service quên verify là toang toàn hệ
    thống — đây chính là rủi ro của "trust downstream" thay vì "verify tại gateway".
18. *"RabbitMQ down giữa lúc publish — dữ liệu đó thế nào?"* → Tuỳ publisher confirm có bật hay
    không; không có thì mất message im lặng.
19. *"App.partners được set trên mỗi connection từ pool — điều gì xảy ra nếu connection bị reuse
    sai giữa hai request của hai tenant khác nhau?"* → Đây là rủi ro thật của kỹ thuật session-variable-per-tenant:
    nếu framework không reset `app.partners` khi trả connection về pool, request sau có thể đọc
    nhầm dữ liệu tenant trước — phải đảm bảo `TenantHikariDataSource` reset session state ở mọi
    lần borrow/return connection.

---

## 6. Checklist trước khi mang vào phỏng vấn

- [ ] Nói đúng cơ chế tenant isolation thật (§4) — không lẫn với SystemType/phân kênh
- [ ] Chuẩn bị câu chuyện "gateway có nhưng thiếu nửa" (§5.2 Q10) — mạnh hơn "chưa có gateway"
- [ ] Chuẩn bị nói về Spring Batch + Quartz (§5.2 Q11) làm cầu nối tới yêu cầu "large-scale batch
      processing" của JD khi chưa có Spark thật
- [ ] Ôn khái niệm ECS ↔ K8s — vẫn là gap thật, chưa đổi so với bản 1
- [ ] Quyết định trước: gọi tên "all-in-one-v2" cụ thể hay chỉ dùng pattern không gọi tên (dự án
      không nằm trong CV)
