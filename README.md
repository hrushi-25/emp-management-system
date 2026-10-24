# Employee Management System

A simple web-based **Employee Management System** developed as a Java mini-project. The application provides a basic interface for managing employee records, viewing employee information, and performing common employee management operations.

The project is designed to demonstrate fundamental **Java programming, web development, HTTP server handling, HTML, CSS, JavaScript, and browser-based data storage** without using complex frameworks or external databases.

---

## 🚀 Features

- Employee Management Dashboard
- View employee records
- Add new employees
- Edit employee information
- Delete employee records
- Search employees
- Filter employees by department and status
- View employee details
- Employee salary information
- Basic employee statistics
- Simple admin login
- Browser-based temporary data storage using `localStorage`
- Clean and responsive user interface

---

## 🛠️ Technologies Used

### Backend
- Java 21
- Java `HttpServer`

### Frontend
- HTML5
- CSS3
- JavaScript

### Data Storage
- Browser `localStorage`

### Development Tools
- Visual Studio Code
- Git
- GitHub

---

## 📂 Project Structure

```text
Employee-Management-System/
│
├── src/
│   └── EmployeeServer.java
│
├── web/
│   ├── index.html
│   ├── css/
│   └── js/
│
├── .gitignore
└── README.md
```

---

## ⚙️ Requirements

Before running the project, make sure you have:

- Java JDK 21 or later
- Git
- A modern web browser
- Visual Studio Code (recommended)

Check your Java installation:

```bash
java -version
```

Check the Java compiler:

```bash
javac -version
```

---

## ▶️ How to Run

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/Employee-Management-System.git
```

### 2. Open the project

```bash
cd Employee-Management-System
```

### 3. Compile the Java server

```bash
javac src/EmployeeServer.java
```

### 4. Start the server

```bash
java -cp src EmployeeServer
```

You should see:

```text
Employee Management System running at http://localhost:8080
```

### 5. Open the application

Open your browser and visit:

```text
http://localhost:8080
```

---

## 👨‍💼 Employee Information

The system is designed to manage employee information such as:

- Employee ID
- Full Name
- Age
- Gender
- Email
- Phone
- Department
- Designation
- Joining Date
- Salary
- Address
- Employment Status

---

## 💾 Data Storage

This project does not use MySQL or another external database.

Employee records are intended to be stored temporarily in the browser using:

```javascript
localStorage
```

Example:

```javascript
localStorage.setItem(
    "employees",
    JSON.stringify(employees)
);
```

Data can then be retrieved using:

```javascript
const employees =
    JSON.parse(localStorage.getItem("employees")) || [];
```

This approach keeps the project lightweight and easy to run for demonstration purposes.

---

## 🔐 Admin Login

The project includes a basic administrator login concept for demonstration.

Example credentials:

```text
Username: admin
Password: admin123
```

> **Note:** This authentication approach is intended only for an academic/demo project and should not be considered production-level security.

---

## 📊 Dashboard

The dashboard can display basic employee statistics such as:

- Total Employees
- Active Employees
- Inactive Employees
- Employees by Department
- Average Salary

---

## 🔄 CRUD Operations

The system follows the basic CRUD model:

| Operation | Description |
|---|---|
| Create | Add a new employee |
| Read | View employee records |
| Update | Edit employee information |
| Delete | Remove an employee |

---

## 🎯 Project Objective

The main objective of this project is to demonstrate how Java can be used to create a simple web-based application while integrating frontend technologies and browser-based data storage.

The project also provides practical understanding of:

- Java programming
- HTTP server concepts
- Client-server communication
- HTML and CSS
- JavaScript
- CRUD operations
- Local storage
- Git and GitHub

---

## 🔮 Future Improvements

Possible future improvements include:

- MySQL/PostgreSQL database integration
- Secure authentication
- Role-based access control
- REST API
- Spring Boot backend
- Employee profile images
- Advanced reporting
- Export reports to PDF/Excel
- Cloud deployment
- User activity logs

---

## 👨‍💻 Authors

**Employee Management System**

Developed as a Java academic mini-project.

---

## 📄 License

This project is created for educational and academic purposes.