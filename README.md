# 💼 FinSec Accounting

FinSec Accounting is a zero-dependency desktop application designed to simplify financial management and reporting for organizations. 

## 📖 The "Why"
Every organization relies on a team to manage its finances. I built this project after serving as a Financial Secretary and realizing how heavy the reliance is on manual spreadsheets. I wanted to build a system that:
* Empowers anyone to take on the role, even without advanced spreadsheet skills.
* Eliminates human error to guarantee accurate, standardized records.
* Secures confidential transactions from third parties.

FinSec Accounting replaces scattered spreadsheets with an intuitive, secure UI. It also serves as a hands-on project to refine my skills in software architecture and desktop-to-cloud data persistence.

## ✨ Features
* **Intuitive UI:** Easily record and categorize income and expenses.
* **Zero-Dependency Release:** Packaged with an embedded Java Runtime via `jpackage`. Users don't need Java installed to run it.
* **Secure & Accurate:** Standardized data entry to protect sensitive records.

## 🛠️ Tech Stack
* **Frontend:** JavaFX
* **Backend:** Spring Boot (Java)
* **Database:** MongoDB
* **Packaging:** Maven & `jpackage`

## 🚀 How to Run (Portable Release)
1. Download the latest `.zip` release from the **Releases** page on GitHub.
2. Extract the folder to your desired location.
3. Ensure you have a local MongoDB instance running on port `27017`.
4. Double-click `FinSec Accounting.exe` to launch the application.

## 💻 Building from Source
```bash
# 1. Clone the repo
git clone [https://github.com/Ikedeze/finsec-accounting.git](https://github.com/Ikedeze/finsec-accounting.git)
cd finsec-accounting

# 2. Build the executable JAR
.\mvnw clean package -DskipTests

# 3. Create the Windows App-Image (Requires jpackage)
jpackage --type app-image --name "FinSec Accounting" --input target --main-jar financial-secretary-app-0.0.1-SNAPSHOT.jar --main-class org.springframework.boot.loader.launch.JarLauncher --dest dist
