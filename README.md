# SAP BTP XSUAA Spring Boot 3 Resource Server

This repository contains a demo Spring Boot 3 Java backend configured as an OAuth2 Resource Server using SAP BTP XSUAA authentication. 

It provides two simple REST endpoints to demonstrate access control:
* `GET /open`: A public endpoint accessible without any authentication.
* `GET /secure`: A secure endpoint that requires a valid JWT (Bearer token) issued by your SAP XSUAA service. It extracts and displays the `user_name` from the token.

---

## Prerequisites

* **Java 17**
* **Maven 3.6+**
* **Cloud Foundry CLI (`cf`)** (for deployment)
* **Cloud MTA Build Tool (`mbt`)** (for building the deployment archive)

---

## 💻 Local Development & Testing

To run this application locally, you must provide your SAP XSUAA service key credentials so the application can validate JWT tokens offline.

### 1. Configure Local Credentials
Create or update the `src/main/resources/application-local.properties` file with your XSUAA service binding details. 

*(Note: This file is ignored by `.mtaignore` and should **not** be committed to your remote repository if it contains real secrets!)*

Example format:
```properties
sap.security.services.xsuaa.clientid=your-client-id
sap.security.services.xsuaa.clientsecret=your-client-secret
sap.security.services.xsuaa.url=https://your-tenant.authentication.sap.hana.ondemand.com
sap.security.services.xsuaa.identityzone=your-tenant
sap.security.services.xsuaa.tenantmode=dedicated
sap.security.services.xsuaa.xsappname=your-xsappname!t1234
# ... include other fields from your service key
# For multiline properties like the RSA verification key, use \n\ to preserve line breaks:
sap.security.services.xsuaa.verificationkey=-----BEGIN PUBLIC KEY-----\n\
MIIBIjANBgkqhkiG9w0B...\n\
-----END PUBLIC KEY-----
```

### 2. Run the Application
Because credentials shouldn't be hardcoded in the main `application.properties`, you must explicitly activate the `local` profile to load your `application-local.properties` file.

Run the following from your terminal:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
*(Alternatively, set the Active Profile to `local` in your IDE's Run Configuration).*

### 3. Test the Endpoints
* **Public Endpoint:** Open your browser to `http://localhost:8080/open`
* **Secure Endpoint:** Attempting to navigate to `http://localhost:8080/secure` in your browser will result in a 401 Unauthorized error. 
  * To test it successfully, obtain a valid JWT token from your XSUAA instance (e.g., using Postman to request a token via Client Credentials or Password grant).
  * Make a request to the secure endpoint passing the token in the HTTP Headers: `Authorization: Bearer <YOUR_TOKEN>`

---

## ☁️ Cloud Foundry Deployment

The project is structured as a Multi-Target Application (MTA). When deployed, Cloud Foundry will automatically activate the `cloud` profile and inject the `VCAP_SERVICES` environment variables into the application.

### Deployment Steps
1. **Log in to Cloud Foundry:**
   ```bash
   cf login -a <your-api-endpoint> -o <org> -s <space>
   ```
2. **Build the MTA Archive:**
   Run the MTA build tool in the root of the project to generate the `.mtar` package:
   ```bash
   mbt build
   ```
3. **Deploy the Archive:**
   Deploy the generated archive to SAP BTP:
   ```bash
   cf deploy mta_archives/springboot-resource-server-xsuaa_0.0.1.mtar
   ```

### Technical Notes on Spring Boot 3 + SAP BTP Compatibility
Deploying modern Spring Boot 3 apps to SAP Cloud Foundry requires some specific configurations to avoid buildpack interference. This project includes the following fixes:

* **Java 17 Enforcement:** `mta.yaml` explicitly requests Java 17 via `JBP_CONFIG_OPEN_JDK_JRE`.
* **Disabled Auto-Reconfiguration:** The Cloud Foundry Java Buildpack injects a legacy `spring-auto-reconfiguration` framework that breaks Spring Boot 3 property resolution. This is disabled in `mta.yaml` via `JBP_CONFIG_SPRING_AUTO_RECONFIGURATION: '{enabled: false}'`.
* **Explicit VCAP Processing:** To guarantee the SAP `JwtDecoder` bean initializes successfully, `SecurityConfig.java` is annotated with `@PropertySource(factory = IdentityServicesPropertySourceFactory.class...` to aggressively parse the `VCAP_SERVICES` variable during context initialization.
* **SAP Auth Converter:** A custom `SecurityFilterChain` is provided, injecting SAP's `Converter<Jwt, AbstractAuthenticationToken>` to natively parse XSUAA scopes and roles.
