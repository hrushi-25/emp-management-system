package com.employee.management;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/employees")
public class EmployeeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        response.getWriter().println("""
            <html>
            <head>
                <title>Employee Management System</title>
            </head>
            <body>
                <h1>Employee Management System</h1>
                <p>Employee Servlet is working!</p>
            </body>
            </html>
            """);
    }
}