# 💼 FinSec Accounting

FinSec Accounting is a zero-dependency desktop application designed to simplify financial management and reporting for organizations. 

## 📖 The "Why"
I built this project after serving as a Financial Secretary and realizing how heavily the role relies on complex Excel spreadsheets[cite: 2]. It made me reflect:
* What if the person taking this role doesn't know Excel?[cite: 2]
* Would they have to pay a third party to handle confidential transactions?[cite: 2]
* How do we guarantee the records are 100% accurate and human-error free?[cite: 2]

FinSec Accounting replaces scattered spreadsheets with an intuitive, secure UI—no advanced spreadsheet skills required. It also serves as a hands-on project to refine my skills in software architecture and desktop-to-cloud data persistence.

## ✨ Features
* **Intuitive UI:** Easily record and categorize income and expenses.
* **Zero-Dependency Release:** Packaged with an embedded Java Runtime via `jpackage`. Users don't need Java installed to run it.
* **Secure & Accurate:** Standardized data entry to eliminate human error and protect sensitive records.

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

## 🗺️ Roadmap
- [ ] Migrate database to **MongoDB Atlas** for full cloud connectivity.
- [ ] Implement PDF and Excel report generation.
- [ ] Transition from local data to real-time cloud synchronization.
