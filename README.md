💼 FinSec Accounting

FinSec Accounting is a zero-dependency desktop application designed to simplify financial management and reporting for organizations.

📖 The "Why"
I built this project after serving as a financial secretary and realizing how heavily the role relies on complex Excel spreadsheets. I wanted to answer a few questions:
•  What if the person taking this role doesn't know Excel?
•  How do we eliminate human error and ensure accurate, standardized records?
FinSec Accounting replaces scattered spreadsheets with an intuitive, secure UI—no advanced spreadsheet skills required. It also serves as a hands-on project to refine my skills in software architecture and desktop-to-cloud data persistence.

✨ Features
•  Intuitive UI: Easily record and categorize income and expenses.
•  Zero-Dependency Release: Packaged with an embedded Java Runtime (jpackage). No need to install Java to run it.
•  Cloud-Ready: Built with MongoDB to allow easy migration from local storage to cloud sync.
•  Secure: Confidential transaction handling to protect sensitive data.

🚀 Quick Start (Playable Release)
Want to try it out without building from source?
1. Download the latest .zip from the Releases Page.
2. Extract the folder.
3. Make sure MongoDB is running locally on port 27017.
4. Double-click FinSec Accounting.exe to launch!

💻 Building from Source
# 1. Clone the repo
git clone https://github.com/Ikedeze/finsec-accounting.git
cd finsec-accounting

# 2. Build the executable JAR
.\mvnw clean package -DskipTests

# 3. Create the Windows App-Image (Requires jpackage)
jpackage --type app-image --name "FinSec Accounting" --input target --main-jar financial-secretary-app-0.0.1-SNAPSHOT.jar --main-class org.springframework.boot.loader.launch.JarLauncher --dest dist


🗺️ Roadmap
•  [ ] Migrate local MongoDB to MongoDB Atlas (Cloud).
•  [ ] Export financial reports to PDF.
•  [ ] Build a companion mobile application.

👤 Author
Ikechukwu Udeze
Java Developer | Aspiring Cybersecurity Professional
LinkedIn • GitHub
