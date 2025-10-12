# Heroku Vercel-like Routing Setup

## 1. Prerequisites

* Heroku account (verified)
* Custom domain registered (e.g., `wareality.tech`)
* Wildcard domain support in DNS provider (CNAME/ALIAS/ANAME)
* Spring Boot project with a proxy controller (like `S3ReverseProxy`)
* AWS S3 bucket configured to serve static files

---

## 2. Deploy Spring Boot Proxy App to Heroku

1. **Login to Heroku CLI**

```bash
heroku login
```

2. **Create a Heroku app**

```bash
heroku create s3proxy
```

3. **Deploy Spring Boot app to Heroku**

```bash
git add .
git commit -m "Initial Spring proxy commit"
git push heroku main
```

4. **Verify deployment**

```bash
heroku logs --tail
heroku open
```

---

## 3. Add Custom Domain

1. **Add wildcard domain**

```bash
heroku domains:add '*.wareality.tech' -a s3proxy
```

* Note the DNS Target provided by Heroku.
* a DNS Target from heroku will be provided like this bjective-ape-jo0i3z43jfk10r309zmmwzew.herokudns.com
* 


---

## 4. Configure DNS

1. Go to your DNS provider.


### 2. Add CNAME for Wildcard

| Type  | Name | Value (DNS Target)                                         |
|-------|------|------------------------------------------------------------|
| CNAME | *    | objective-ape-jo0i3z43jfk10r309zmmwzew.herokudns.com      |

4. Verify DNS propagation:

```bash
dig project.wareality.tech
```

---

## 5. Enable SSL with ACM

```bash
heroku certs:auto -a s3proxy
heroku certs:auto:enable -a s3proxy
heroku certs -a s3proxy
```

---

## 6. Spring Proxy Setup

* Detect subdomain:

```java
String hostname = request.getServerName();
String subdomain = hostname.split("\\.")[0];
```

* Determine staging or production:

```java
if (subdomain.contains("--")) {
    String[] parts = subdomain.split("--", 2);
    String deploymentId = parts[0];
    String projectId = parts[1];
    targetBase = BASE_PATH + "/" + projectId + "/deployments/" + deploymentId;
} else {
    String projectId = subdomain;
    targetBase = BASE_PATH + "/" + projectId + "/production";
}
```

* Proxy requests to S3 bucket.
* Handle index.html fallback, content types, caching headers.
* Example BASE_PATH:

```java
private static final String BASE_PATH = "https://pushly-clone-outputs.s3.ap-south-1.amazonaws.com/__outputs";
```

---

## 7. Test Routing

* Production:

```
https://project.wareality.tech/ -> https://pushly-clone-outputs.s3.ap-south-1.amazonaws.com/__outputs/project/production/index.html
```

* Staging:

```
https://deployment--project.wareality.tech/ -> https://pushly-clone-outputs.s3.ap-south-1.amazonaws.com/__outputs/project/deployments/deployment
```

---

## 8. Best Practices

1. Keep wildcard DNS and Heroku DNS target in sync.
2. Enable logging in Spring for debugging subdomain routing.
3. Ensure S3 bucket public read or proper CORS headers.
4. Cache static files for performance.
5. Optionally, redirect root domain to www or preferred subdomain.

---

## 9. Useful Heroku CLI Commands

* Check domains:

```bash
heroku domains -a s3proxy
```

* Remove domain:

```bash
heroku domains:remove www.wareality.tech -a s3proxy
```

* Wait for domain addition:

```bash
heroku domains:wait '*.wareality.tech' -a s3proxy
```

* Check SSL certificates:

```bash
heroku certs -a s3proxy
```

---

## ✅ Summary

* **Heroku app**: Spring Boot proxy
* **Wildcard domain**: `*.wareality.tech` → Heroku DNS target
* **Routing**: `subdomain → S3 bucket path`
* **SSL**: ACM handles HTTPS automatically
* **Vercel-like behavior**: Any subdomain points dynamically to its S3 folder
