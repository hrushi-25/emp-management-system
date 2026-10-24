import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Employee Management System (single file, no dependencies, Java 17+).
 * Compile: javac -encoding UTF-8 EmployeeServer.java
 * Run:     java EmployeeServer      ->  http://localhost:8080
 * Login:   admin / admin123
 *
 * UI: glassmorphism, motion design, 1.2s skeleton loader,
 * palette  #6C151E (wine)  #0F3D3A (deep teal)  #3F5DAB (royal blue).
 */
public class EmployeeServer {

    // ---------- Data ----------
    static class Emp {
        String id, name, email, phone, dept, desig, status = "Active";
        double salary;
        LocalDate joined;
        int presentDays;
    }

    static class Leave {
        int id;
        String empId, type, from, to, reason, status = "Pending";
    }

    static final Map<String, Emp> emps = new LinkedHashMap<>();
    static final List<String> depts = new ArrayList<>(List.of("IT", "HR", "Finance", "Marketing", "Operations"));
    static final Map<String, String> att = new HashMap<>();   // today's attendance
    static final Map<String, String> paid = new HashMap<>();  // payroll status this month
    static final List<Leave> leaves = new ArrayList<>();
    static final Set<String> sessions = ConcurrentHashMap.newKeySet();
    static String adminUser = "admin", adminPass = "admin123";
    static int nextEmp = 11, nextLeave = 1;

    static void seed() {
        for (String nd : List.of("Sales", "Customer Support", "Legal", "Design", "Research & Development"))
            if (!depts.contains(nd)) depts.add(nd);

        String[][] d = {
                {"Rahul Sharma", "IT", "Software Developer", "45000"},
                {"Priya Patil", "HR", "HR Executive", "38000"},
                {"Amit Shah", "Finance", "Accountant", "42000"},
                {"Neha Joshi", "Marketing", "Marketing Executive", "40000"},
                {"Sneha Kulkarni", "IT", "QA Engineer", "39000"},
                {"Vikram Rao", "Operations", "Operations Manager", "62000"},
                {"Anjali Desai", "Finance", "Financial Analyst", "52000"},
                {"Rohan Mehta", "IT", "DevOps Engineer", "58000"},
                {"Kavita Nair", "HR", "Recruiter", "36000"},
                {"Suresh Iyer", "Marketing", "SEO Specialist", "37000"},
                {"Arjun Verma", "Sales", "Sales Executive", "41000"},
                {"Pooja Bhatt", "Sales", "Sales Manager", "68000"},
                {"Deepak Kulkarni", "Sales", "Business Development Associate", "35000"},
                {"Meera Reddy", "Customer Support", "Support Executive", "30000"},
                {"Karan Malhotra", "Customer Support", "Team Lead - Support", "48000"},
                {"Isha Gupta", "Customer Support", "Support Executive", "31000"},
                {"Adv. Sanjay Pillai", "Legal", "Legal Advisor", "75000"},
                {"Ritu Saxena", "Legal", "Compliance Officer", "56000"},
                {"Tanvi Shetty", "Design", "UI/UX Designer", "50000"},
                {"Nikhil Pawar", "Design", "Graphic Designer", "38000"},
                {"Divya Menon", "Design", "Product Designer", "55000"},
                {"Aditya Kapoor", "Research & Development", "ML Engineer", "72000"},
                {"Shruti Bansal", "Research & Development", "Data Scientist", "70000"},
                {"Manish Tiwari", "Operations", "Logistics Coordinator", "34000"},
                {"Farhan Sheikh", "IT", "Backend Developer", "54000"}};

        String[] st = {"Present", "Present", "Absent", "Present", "On Leave", "Present", "Present", "Present", "Absent", "Present"};
        for (int i = 0; i < d.length; i++) {
            Emp e = new Emp();
            e.id = String.format("EMP%03d", i + 1);
            e.name = d[i][0];
            e.dept = d[i][1];
            e.desig = d[i][2];
            e.salary = Double.parseDouble(d[i][3]);
            e.email = d[i][0].toLowerCase().replaceAll("[^a-z ]", "").trim().replace(" ", ".") + "@company.com";
            e.phone = "98200" + String.format("%05d", 11111 * (i + 1) % 100000);
            e.joined = LocalDate.now().minusDays(90L * (i + 1));
            e.presentDays = 14 + (i * 3) % 8;
            e.status = (i % 12 == 11) ? "Inactive" : "Active";
            emps.put(e.id, e);
            att.put(e.id, st[i % st.length]);
            paid.put(e.id, i % 3 == 0 ? "Paid" : "Pending");
        }
        nextEmp = d.length + 1;

        addLeave("EMP005", "Sick Leave", 0, 2, "Fever and rest", "Approved");
        addLeave("EMP002", "Casual Leave", 5, 6, "Family function", "Pending");
        addLeave("EMP008", "Annual Leave", 10, 15, "Vacation trip", "Pending");
        addLeave("EMP003", "Sick Leave", -6, -5, "Dental surgery", "Approved");
        addLeave("EMP010", "Casual Leave", -12, -12, "Personal work", "Rejected");
        addLeave("EMP012", "Annual Leave", 20, 27, "Family trip to Goa", "Pending");
        addLeave("EMP015", "Sick Leave", -3, -2, "Viral infection", "Approved");
        addLeave("EMP019", "Casual Leave", 3, 3, "Bank and passport work", "Pending");
        addLeave("EMP022", "Maternity/Paternity", 30, 60, "Paternity leave", "Approved");
        addLeave("EMP024", "Casual Leave", -20, -19, "Cousin's wedding", "Rejected");
    }

    static void addLeave(String emp, String type, int fromOff, int toOff, String reason, String status) {
        Leave l = new Leave();
        l.id = nextLeave++;
        l.empId = emp;
        l.type = type;
        l.from = LocalDate.now().plusDays(fromOff).toString();
        l.to = LocalDate.now().plusDays(toOff).toString();
        l.reason = reason;
        l.status = status;
        leaves.add(l);
    }

    // ---------- Server ----------
    public static void main(String[] args) throws IOException {
        seed();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", EmployeeServer::handle);
        server.start();
        System.out.println("Employee Management System running at http://localhost:8080  (admin / admin123)");
    }

    static void handle(HttpExchange x) throws IOException {
        try {
            String p = x.getRequestURI().getPath(), m = x.getRequestMethod();
            Map<String, String> q = parse(x.getRequestURI().getRawQuery());
            Map<String, String> f = m.equals("POST")
                    ? parse(new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)) : Map.of();

            if (p.equals("/login")) {
                if (m.equals("POST")) {
                    if (adminUser.equals(f.get("username")) && adminPass.equals(f.get("password"))) {
                        String t = UUID.randomUUID().toString();
                        sessions.add(t);
                        x.getResponseHeaders().add("Set-Cookie", "SID=" + t + "; Path=/; HttpOnly");
                        redirect(x, "/");
                    } else send(x, loginPage("Invalid username or password"), "text/html");
                } else send(x, loginPage(null), "text/html");
                return;
            }
            if (!loggedIn(x)) { redirect(x, "/login"); return; }
            String msg = q.get("msg");

            switch (m + " " + p) {
                case "GET /logout" -> {
                    sessions.remove(sid(x));
                    redirect(x, "/login");
                }
                case "GET /" -> send(x, dashboard(msg), "text/html");
                case "GET /employees" -> send(x, employeeList(q, msg), "text/html");
                case "GET /employees/add" -> send(x, page("Add Employee", "/employees/add", msg,
                        "<h2>Add Employee</h2>" + empForm(null, "/employees/add")), "text/html");
                case "POST /employees/add" -> {
                    Emp e = new Emp();
                    e.id = String.format("EMP%03d", nextEmp++);
                    String err = fill(e, f);
                    if (err != null) { nextEmp--; redirect(x, "/employees/add?msg=" + enc(err)); break; }
                    emps.put(e.id, e);
                    att.put(e.id, "Not Marked");
                    paid.put(e.id, "Pending");
                    redirect(x, "/employees?msg=" + enc("Employee " + e.name + " added (" + e.id + ")"));
                }
                case "GET /employees/view" -> send(x, viewEmp(q.get("id"), msg), "text/html");
                case "GET /employees/edit" -> {
                    Emp e = emps.get(q.get("id"));
                    if (e == null) { redirect(x, "/employees?msg=Not+found"); break; }
                    send(x, page("Edit Employee", "/employees", msg, "<h2>Edit " + esc(e.name) + "</h2>"
                            + empForm(e, "/employees/edit?id=" + e.id)), "text/html");
                }
                case "POST /employees/edit" -> {
                    Emp e = emps.get(q.get("id"));
                    String err = e == null ? "Employee not found" : fill(e, f);
                    redirect(x, err != null ? "/employees?msg=" + enc(err) : "/employees?msg=" + enc("Employee updated"));
                }
                case "POST /employees/delete" -> {
                    String id = f.get("id");
                    emps.remove(id); att.remove(id); paid.remove(id);
                    leaves.removeIf(l -> l.empId.equals(id));
                    redirect(x, "/employees?msg=" + enc("Employee deleted"));
                }
                case "GET /departments" -> send(x, departments(msg), "text/html");
                case "POST /departments/add" -> {
                    String n = f.getOrDefault("name", "").trim();
                    String r = n.isEmpty() ? "Name required" : depts.contains(n) ? "Department exists" : null;
                    if (r == null) { depts.add(n); r = "Department added"; }
                    redirect(x, "/departments?msg=" + enc(r));
                }
                case "POST /departments/delete" -> {
                    String n = f.get("name");
                    boolean used = emps.values().stream().anyMatch(e -> e.dept.equals(n));
                    if (!used) depts.remove(n);
                    redirect(x, "/departments?msg=" + enc(used ? "Cannot delete: department has employees" : "Department deleted"));
                }
                case "GET /attendance" -> send(x, attendance(msg), "text/html");
                case "POST /attendance/mark" -> {
                    if (emps.containsKey(f.get("id"))) att.put(f.get("id"), f.get("status"));
                    redirect(x, "/attendance?msg=" + enc("Attendance updated"));
                }
                case "POST /attendance/all" -> {
                    emps.values().stream().filter(e -> e.status.equals("Active")
                            && !att.get(e.id).equals("On Leave")).forEach(e -> att.put(e.id, "Present"));
                    redirect(x, "/attendance?msg=" + enc("All active employees marked present"));
                }
                case "GET /leaves" -> send(x, leavePage(msg), "text/html");
                case "POST /leaves/add" -> {
                    if (emps.containsKey(f.get("emp")) && !f.getOrDefault("from", "").isEmpty() && !f.getOrDefault("to", "").isEmpty()) {
                        Leave l = new Leave();
                        l.id = nextLeave++;
                        l.empId = f.get("emp"); l.type = f.get("type"); l.from = f.get("from");
                        l.to = f.get("to"); l.reason = f.getOrDefault("reason", "");
                        leaves.add(l);
                        redirect(x, "/leaves?msg=" + enc("Leave request submitted"));
                    } else redirect(x, "/leaves?msg=" + enc("Please fill all fields"));
                }
                case "POST /leaves/action" -> {
                    int id = Integer.parseInt(f.get("id"));
                    leaves.stream().filter(l -> l.id == id).findFirst().ifPresent(l -> l.status = f.get("action"));
                    redirect(x, "/leaves?msg=" + enc("Leave " + f.get("action").toLowerCase()));
                }
                case "GET /payroll" -> send(x, payroll(msg), "text/html");
                case "POST /payroll/pay" -> {
                    if (f.get("id").equals("ALL")) paid.replaceAll((k, v) -> "Paid");
                    else paid.put(f.get("id"), "Paid");
                    redirect(x, "/payroll?msg=" + enc("Salary marked as paid"));
                }
                case "GET /reports" -> send(x, reports(msg), "text/html");
                case "GET /reports/export.csv" -> {
                    StringBuilder sb = new StringBuilder("ID,Name,Email,Phone,Department,Designation,Salary,Joined,Status\n");
                    emps.values().forEach(e -> sb.append(String.join(",", e.id, e.name, e.email, e.phone, e.dept,
                            e.desig, String.valueOf((long) e.salary), e.joined.toString(), e.status)).append("\n"));
                    x.getResponseHeaders().set("Content-Disposition", "attachment; filename=employees.csv");
                    send(x, sb.toString(), "text/csv");
                }
                case "GET /settings" -> send(x, page("Settings", "/settings", msg,
                        "<h2>Admin Settings</h2><div class='panel' style='max-width:440px'><p>Logged in as <b>"
                                + esc(adminUser) + "</b></p><form method='post' action='/settings'>"
                                + "<label>Current Password</label><input type='password' name='old' required>"
                                + "<label>New Password</label><input type='password' name='new' minlength='4' required>"
                                + "<div style='margin-top:18px'><button class='btn'>Change Password</button></div></form></div>"), "text/html");
                case "POST /settings" -> {
                    boolean ok = adminPass.equals(f.get("old"));
                    if (ok) adminPass = f.get("new");
                    redirect(x, "/settings?msg=" + enc(ok ? "Password changed" : "Current password is wrong"));
                }
                default -> send(x, page("Not Found", "", null, "<h2>404 - Page not found</h2>"
                        + "<div class='panel'><p>The page you are looking for has drifted out of orbit.</p>"
                        + "<a class='btn' href='/'>Go to Dashboard</a></div>"), "text/html");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            send(x, "<h2>Server error: " + esc(String.valueOf(ex)) + "</h2><a href='/'>Home</a>", "text/html");
        }
    }

    // ---------- Pages ----------
    static String dashboard(String msg) {
        long active = emps.values().stream().filter(e -> e.status.equals("Active")).count();
        long present = att.values().stream().filter(s -> s.equals("Present")).count();
        long pending = leaves.stream().filter(l -> l.status.equals("Pending")).count();
        double payroll = emps.values().stream().mapToDouble(e -> net(e)).sum();
        StringBuilder sb = new StringBuilder("<h2>Dashboard</h2><p class='sub'>Live overview of your workforce</p><div class='cards'>");
        sb.append(card("Total Employees", String.valueOf(emps.size()), "/employees"))
                .append(card("Active", String.valueOf(active), "/employees?q=Active"))
                .append(card("Present Today", String.valueOf(present), "/attendance"))
                .append(card("Pending Leaves", String.valueOf(pending), "/leaves"))
                .append(card("Monthly Payroll", money(payroll), "/payroll"))
                .append(card("Departments", String.valueOf(depts.size()), "/departments")).append("</div>");

        // department bars
        sb.append("<div class='grid2'><div class='panel'><h3>Employees by Department</h3>");
        int max = Math.max(1, depts.stream().mapToInt(DeptCount::of).max().orElse(1));
        for (String d : depts)
            sb.append("<div class='bar'><span>").append(esc(d)).append("</span><div><i data-w='")
                    .append(DeptCount.of(d) * 100 / max).append("'></i></div><b>").append(DeptCount.of(d)).append("</b></div>");

        // attendance donut
        String[] ks = {"Present", "Absent", "On Leave", "Not Marked"};
        String[] cols = {"#2fa89c", "#b8414f", "#7d98e0", "rgba(255,255,255,.25)"};
        int[] c = new int[4];
        for (int i = 0; i < 4; i++) { final String k = ks[i]; c[i] = (int) att.values().stream().filter(v -> v.equals(k)).count(); }
        int total = Math.max(1, att.size());
        StringBuilder cg = new StringBuilder("conic-gradient(");
        double a = 0;
        for (int i = 0; i < 4; i++) {
            double b = a + c[i] * 360.0 / total;
            if (i > 0) cg.append(",");
            cg.append(cols[i]).append(" ").append(String.format(Locale.US, "%.1fdeg %.1fdeg", a, b));
            a = b;
        }
        cg.append(")");
        sb.append("</div><div class='panel'><h3>Today's Attendance</h3><div class='donut-wrap'><div class='donut' style='background:")
                .append(cg).append("'><div class='dc'><b>").append(c[0] * 100 / total).append("%</b><small>present</small></div></div><div class='legend'>");
        for (int i = 0; i < 4; i++)
            sb.append("<p><i style='background:").append(cols[i]).append("'></i>").append(ks[i]).append(" <b>").append(c[i]).append("</b></p>");
        sb.append("</div></div></div></div>");

        sb.append("<div class='panel'><h3>Recent Leave Requests</h3><table><tr><th>Employee</th><th>Type</th><th>Status</th></tr>");
        leaves.stream().sorted(Comparator.comparingInt((Leave l) -> -l.id)).limit(5).forEach(l ->
                sb.append("<tr><td>").append(esc(nm(l.empId))).append("</td><td>").append(esc(l.type))
                        .append("</td><td>").append(badge(l.status)).append("</td></tr>"));
        sb.append("</table></div><div class='panel'><h3>Quick Actions</h3>")
                .append("<a class='btn' href='/employees/add'>Add Employee</a> <a class='btn green' href='/attendance'>Mark Attendance</a> ")
                .append("<a class='btn dark' href='/leaves'>Review Leaves</a> <a class='btn dark' href='/reports/export.csv'>Export CSV</a></div>");
        return page("Dashboard", "/", msg, sb.toString());
    }

    static class DeptCount {
        static int of(String d) { return (int) emps.values().stream().filter(e -> e.dept.equals(d)).count(); }
    }

    static String employeeList(Map<String, String> q, String msg) {
        String s = q.getOrDefault("q", "").toLowerCase(), d = q.getOrDefault("dept", "");
        StringBuilder sb = new StringBuilder("<h2>Employees</h2><form class='bar2' method='get' action='/employees'>"
                + "<input name='q' placeholder='Search name, ID, email, status...' value='" + esc(q.getOrDefault("q", "")) + "'>"
                + "<select name='dept'><option value=''>All Departments</option>");
        for (String x : depts) sb.append("<option").append(x.equals(d) ? " selected" : "").append(">").append(esc(x)).append("</option>");
        sb.append("</select><button class='btn'>Search</button> <a class='btn dark' href='/employees'>Reset</a> "
                + "<a class='btn green' href='/employees/add'>+ Add Employee</a></form>"
                + "<table><tr><th>ID</th><th>Name</th><th>Department</th><th>Designation</th><th>Salary</th><th>Status</th><th>Actions</th></tr>");
        int n = 0;
        for (Emp e : emps.values()) {
            String hay = (e.id + e.name + e.email + e.status + e.desig).toLowerCase();
            if (!s.isEmpty() && !hay.contains(s)) continue;
            if (!d.isEmpty() && !e.dept.equals(d)) continue;
            n++;
            sb.append("<tr><td>").append(e.id).append("</td><td><div class='who'>").append(avatar(e.name)).append("<span>")
                    .append(esc(e.name)).append("</span></div></td><td>").append(esc(e.dept))
                    .append("</td><td>").append(esc(e.desig)).append("</td><td>").append(money(e.salary)).append("</td><td>")
                    .append(badge(e.status)).append("</td><td class='act'><a class='sm' href='/employees/view?id=").append(e.id)
                    .append("'>View</a><a class='sm' href='/employees/edit?id=").append(e.id).append("'>Edit</a>")
                    .append(post("/employees/delete", "sm red", "Delete", "Delete " + e.name + "?", "id", e.id)).append("</td></tr>");
        }
        if (n == 0) sb.append("<tr><td colspan='7'>No employees found.</td></tr>");
        return page("Employees", "/employees", msg, sb.append("</table>").toString());
    }

    static String viewEmp(String id, String msg) {
        Emp e = emps.get(id);
        if (e == null) return page("Employee", "/employees", "Employee not found", "<a class='btn' href='/employees'>Back</a>");
        long years = ChronoUnit.DAYS.between(e.joined, LocalDate.now());
        String body = "<h2>" + esc(e.name) + " " + badge(e.status) + "</h2><div class='panel'><table class='kv'>"
                + kv("Employee ID", e.id) + kv("Email", e.email) + kv("Phone", e.phone) + kv("Department", e.dept)
                + kv("Designation", e.desig) + kv("Joined", e.joined + " (" + years + " days)") + kv("Monthly Salary", money(e.salary))
                + kv("Net Pay", money(net(e))) + kv("Today's Attendance", att.getOrDefault(e.id, "Not Marked"))
                + kv("Days Present (month)", String.valueOf(e.presentDays)) + "</table></div>"
                + "<a class='btn' href='/employees/edit?id=" + e.id + "'>Edit</a> <a class='btn dark' href='/employees'>Back</a>";
        return page("Employee", "/employees", msg, body);
    }

    static String empForm(Emp e, String action) {
        boolean n = e == null;
        StringBuilder sb = new StringBuilder("<form class='panel formgrid' method='post' action='" + action + "'>");
        sb.append(inp("Full Name", "name", n ? "" : e.name, "text")).append(inp("Email", "email", n ? "" : e.email, "email"))
                .append(inp("Phone", "phone", n ? "" : e.phone, "text")).append(inp("Designation", "desig", n ? "" : e.desig, "text"))
                .append(inp("Monthly Salary (₹)", "salary", n ? "" : String.valueOf((long) e.salary), "number"))
                .append(inp("Joining Date", "joined", n ? LocalDate.now().toString() : e.joined.toString(), "date"));
        sb.append("<div><label>Department</label><select name='dept'>");
        for (String d : depts) sb.append("<option").append(!n && d.equals(e.dept) ? " selected" : "").append(">").append(esc(d)).append("</option>");
        sb.append("</select></div><div><label>Status</label><select name='status'>");
        for (String s : List.of("Active", "Inactive", "On Leave"))
            sb.append("<option").append(!n && s.equals(e.status) ? " selected" : "").append(">").append(s).append("</option>");
        return sb.append("</select></div><div class='full'><button class='btn'>").append(n ? "Add Employee" : "Save Changes")
                .append("</button> <a class='btn dark' href='/employees'>Cancel</a></div></form>").toString();
    }

    static String fill(Emp e, Map<String, String> f) {
        String name = f.getOrDefault("name", "").trim();
        if (name.isEmpty()) return "Name is required";
        try {
            e.salary = Double.parseDouble(f.getOrDefault("salary", "0"));
            e.joined = LocalDate.parse(f.getOrDefault("joined", LocalDate.now().toString()));
        } catch (Exception ex) { return "Invalid salary or date"; }
        e.name = name; e.email = f.getOrDefault("email", ""); e.phone = f.getOrDefault("phone", "");
        e.dept = f.getOrDefault("dept", depts.get(0)); e.desig = f.getOrDefault("desig", "");
        e.status = f.getOrDefault("status", "Active");
        return null;
    }

    static String departments(String msg) {
        StringBuilder sb = new StringBuilder("<h2>Departments</h2><form class='bar2' method='post' action='/departments/add'>"
                + "<input name='name' placeholder='New department name' required><button class='btn green'>+ Add Department</button></form>"
                + "<table><tr><th>Department</th><th>Employees</th><th>Total Salary</th><th>Head Count Share</th><th>Action</th></tr>");
        for (String d : depts) {
            List<Emp> l = emps.values().stream().filter(e -> e.dept.equals(d)).toList();
            sb.append("<tr><td>").append(esc(d)).append("</td><td>").append(l.size()).append("</td><td>")
                    .append(money(l.stream().mapToDouble(e -> e.salary).sum())).append("</td><td>")
                    .append(emps.isEmpty() ? 0 : l.size() * 100 / emps.size()).append("%</td><td class='act'>")
                    .append("<a class='sm' href='/employees?dept=").append(enc(d)).append("'>View Staff</a>")
                    .append(post("/departments/delete", "sm red", "Delete", "Delete department " + d + "?", "name", d)).append("</td></tr>");
        }
        return page("Departments", "/departments", msg, sb.append("</table>").toString());
    }

    static String attendance(String msg) {
        StringBuilder sb = new StringBuilder("<h2>Attendance - " + LocalDate.now() + "</h2><div class='bar2'>"
                + post("/attendance/all", "btn green", "Mark All Present", "Mark all active employees present?") + "</div>"
                + "<table><tr><th>ID</th><th>Name</th><th>Department</th><th>Today</th><th>Days Present (month)</th><th>Mark</th></tr>");
        for (Emp e : emps.values()) {
            sb.append("<tr><td>").append(e.id).append("</td><td><div class='who'>").append(avatar(e.name)).append("<span>")
                    .append(esc(e.name)).append("</span></div></td><td>").append(esc(e.dept))
                    .append("</td><td>").append(badge(att.getOrDefault(e.id, "Not Marked"))).append("</td><td>").append(e.presentDays)
                    .append("</td><td class='act'>");
            for (String s : List.of("Present", "Absent", "On Leave"))
                sb.append(post("/attendance/mark", "sm", s, null, "id", e.id, "status", s));
            sb.append("</td></tr>");
        }
        return page("Attendance", "/attendance", msg, sb.append("</table>").toString());
    }

    static String leavePage(String msg) {
        StringBuilder sb = new StringBuilder("<h2>Leave Requests</h2><form class='panel formgrid' method='post' action='/leaves/add'>"
                + "<div><label>Employee</label><select name='emp'>");
        emps.values().forEach(e -> sb.append("<option value='").append(e.id).append("'>").append(esc(e.name)).append("</option>"));
        sb.append("</select></div><div><label>Type</label><select name='type'><option>Casual Leave</option><option>Sick Leave</option>"
                + "<option>Annual Leave</option><option>Maternity/Paternity</option></select></div>"
                + inp("From", "from", LocalDate.now().toString(), "date") + inp("To", "to", LocalDate.now().toString(), "date")
                + "<div class='full'><label>Reason</label><input name='reason'></div>"
                + "<div class='full'><button class='btn'>Submit Request</button></div></form>"
                + "<table><tr><th>#</th><th>Employee</th><th>Type</th><th>From</th><th>To</th><th>Days</th><th>Reason</th><th>Status</th><th>Action</th></tr>");
        for (Leave l : leaves) {
            long days = ChronoUnit.DAYS.between(LocalDate.parse(l.from), LocalDate.parse(l.to)) + 1;
            sb.append("<tr><td>").append(l.id).append("</td><td>").append(esc(nm(l.empId))).append("</td><td>").append(esc(l.type))
                    .append("</td><td>").append(l.from).append("</td><td>").append(l.to).append("</td><td>").append(days)
                    .append("</td><td>").append(esc(l.reason)).append("</td><td>").append(badge(l.status)).append("</td><td class='act'>");
            if (l.status.equals("Pending")) {
                sb.append(post("/leaves/action", "sm green", "Approve", null, "id", "" + l.id, "action", "Approved"));
                sb.append(post("/leaves/action", "sm red", "Reject", null, "id", "" + l.id, "action", "Rejected"));
            } else sb.append("-");
            sb.append("</td></tr>");
        }
        return page("Leave Requests", "/leaves", msg, sb.append("</table>").toString());
    }

    static String payroll(String msg) {
        StringBuilder sb = new StringBuilder("<h2>Payroll - " + LocalDate.now().getMonth() + " " + LocalDate.now().getYear() + "</h2>"
                + "<div class='bar2'>" + post("/payroll/pay", "btn green", "Pay All Pending", "Pay all pending salaries?", "id", "ALL") + "</div>"
                + "<table><tr><th>ID</th><th>Name</th><th>Basic</th><th>Allowances (20%)</th><th>Deductions (12%)</th><th>Net Pay</th><th>Status</th><th>Action</th></tr>");
        double total = 0;
        for (Emp e : emps.values()) {
            total += net(e);
            String st = paid.getOrDefault(e.id, "Pending");
            sb.append("<tr><td>").append(e.id).append("</td><td>").append(esc(e.name)).append("</td><td>").append(money(e.salary))
                    .append("</td><td>").append(money(e.salary * 0.2)).append("</td><td>").append(money(e.salary * 0.12))
                    .append("</td><td><b>").append(money(net(e))).append("</b></td><td>").append(badge(st)).append("</td><td class='act'>")
                    .append(st.equals("Paid") ? "-" : post("/payroll/pay", "sm green", "Mark Paid", null, "id", e.id)).append("</td></tr>");
        }
        return page("Payroll", "/payroll", msg, sb.append("</table><p class='total'>Total payroll: <b>").append(money(total)).append("</b></p>").toString());
    }

    static String reports(String msg) {
        StringBuilder sb = new StringBuilder("<h2>Reports</h2><div class='bar2'><a class='btn green' href='/reports/export.csv'>⬇ Export Employees CSV</a>"
                + "<button class='btn dark' onclick='window.print()'>🖨 Print</button></div>");
        double tot = emps.values().stream().mapToDouble(e -> e.salary).sum();
        Optional<Emp> top = emps.values().stream().max(Comparator.comparingDouble(e -> e.salary));
        Optional<Emp> newest = emps.values().stream().max(Comparator.comparing(e -> e.joined));
        sb.append("<div class='cards'>").append(card("Total Salary", money(tot), "#"))
                .append(card("Average Salary", money(emps.isEmpty() ? 0 : tot / emps.size()), "#"))
                .append(card("Highest Paid", top.map(e -> e.name).orElse("-"), "#"))
                .append(card("Newest Joiner", newest.map(e -> e.name).orElse("-"), "#")).append("</div>")
                .append("<div class='panel'><h3>Department Summary</h3><table><tr><th>Department</th><th>Employees</th><th>Total Salary</th><th>Average Salary</th></tr>");
        for (String d : depts) {
            List<Emp> l = emps.values().stream().filter(e -> e.dept.equals(d)).collect(Collectors.toList());
            double t = l.stream().mapToDouble(e -> e.salary).sum();
            sb.append("<tr><td>").append(esc(d)).append("</td><td>").append(l.size()).append("</td><td>").append(money(t))
                    .append("</td><td>").append(money(l.isEmpty() ? 0 : t / l.size())).append("</td></tr>");
        }
        sb.append("</table></div><div class='panel'><h3>Attendance & Leave Summary</h3>");
        for (String s : List.of("Present", "Absent", "On Leave", "Not Marked"))
            sb.append("<p>").append(badge(s)).append(" ").append(att.values().stream().filter(v -> v.equals(s)).count()).append("</p>");
        for (String s : List.of("Pending", "Approved", "Rejected"))
            sb.append("<p>").append(badge(s)).append(" ").append(leaves.stream().filter(l -> l.status.equals(s)).count()).append(" leave requests</p>");
        return page("Reports", "/reports", msg, sb.append("</div>").toString());
    }

    // ---------- Layout & helpers ----------
    static final String SKELETON = "<div id='skel' aria-hidden='true'><div class='sk sk-title'></div><div class='sk sk-sub'></div>"
            + "<div class='sk-cards'>" + "<div class='sk sk-card'></div>".repeat(5) + "</div>"
            + "<div class='sk sk-panel'></div>" + "<div class='sk sk-row'></div>".repeat(6) + "</div>";

    static String page(String title, String active, String msg, String body) {
        String[][] nav = {{"/", "🏠", "Dashboard"}, {"/employees", "👥", "Employees"}, {"/employees/add", "➕", "Add Employee"},
                {"/departments", "🏢", "Departments"}, {"/attendance", "🗓️", "Attendance"}, {"/leaves", "🌴", "Leave Requests"},
                {"/payroll", "💰", "Payroll"}, {"/reports", "📊", "Reports"}, {"/settings", "⚙️", "Settings"}};
        StringBuilder sb = new StringBuilder("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'><title>" + esc(title)
                + " - EMS</title><link rel='preconnect' href='https://fonts.googleapis.com'>"
                + "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap' rel='stylesheet'>"
                + "<style>" + CSS + "</style></head><body><div class='bg'><i></i><i></i><i></i></div><div class='glow' id='glow'></div>"
                + "<aside><div class='logo'><span class='mark'>E</span><span>EMS<small>Admin Console</small></span></div>");
        int i = 0;
        for (String[] n : nav)
            sb.append("<a style='--n:").append(i++).append("' href='").append(n[0]).append("'").append(n[0].equals(active) ? " class='on'" : "").append(">")
                    .append("<em>").append(n[1]).append("</em>").append(n[2]).append("</a>");
        sb.append("<a style='--n:").append(i).append("' href='/logout' class='out'><em>🚪</em>Logout</a></aside><main><header><b>Employee Management System</b>"
                + "<span class='pill'>Welcome, ").append(esc(adminUser)).append(" · ").append(LocalDate.now())
                .append(" · <b id='clock'></b></span></header><section>");
        if (msg != null && !msg.isEmpty()) sb.append("<div class='flash'>").append(esc(msg)).append("</div>");
        sb.append(SKELETON).append("<div id='content' class='hide'>").append(body).append("</div>");
        return sb.append("</section></main><script>").append(SCRIPT).append("</script></body></html>").toString();
    }

    static String loginPage(String err) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<title>Admin Login - EMS</title><link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap' rel='stylesheet'>"
                + "<style>" + CSS + "</style></head><body class='lg'><div class='bg'><i></i><i></i><i></i></div><div class='glow' id='glow'></div>"
                + "<form class='login" + (err != null ? " shake" : "") + "' method='post' action='/login'>"
                + "<span class='mark big'>E</span><h1>Welcome back</h1><p>Employee Management System · Administrator access</p>"
                + (err != null ? "<div class='err'>" + esc(err) + "</div>" : "")
                + "<label>Username</label><input name='username' required autofocus autocomplete='username'>"
                + "<label>Password</label><input type='password' name='password' required autocomplete='current-password'>"
                + "<button class='btn'>Sign In</button><div class='hint'>Demo: admin / admin123</div></form>"
                + "<script>" + SCRIPT + "</script></body></html>";
    }

    static final String CSS = """
            :root{--wine:#6C151E;--teal:#0F3D3A;--blue:#3F5DAB;--wine-l:#b8414f;--teal-l:#2fa89c;--blue-l:#7d98e0;
            --ink:#f1f5f4;--mute:#9db3b0;--glass:rgba(255,255,255,.07);--glass-b:rgba(255,255,255,.16);--bg:#06191a}
            *{box-sizing:border-box}html{scroll-behavior:smooth}
            body{margin:0;font-family:'Inter','Segoe UI',system-ui,Arial,sans-serif;background:var(--bg);color:var(--ink);min-height:100vh;
            overflow-x:hidden;transition:opacity .28s ease,transform .28s ease}
            body.leaving{opacity:0;transform:scale(.99)}
            ::selection{background:var(--blue);color:#fff}
            ::-webkit-scrollbar{width:10px}::-webkit-scrollbar-thumb{background:linear-gradient(var(--wine),var(--blue));border-radius:8px}
            ::-webkit-scrollbar-track{background:transparent}

            /* animated background */
            .bg{position:fixed;inset:0;z-index:-2;overflow:hidden;background:radial-gradient(1200px 700px at 10% -10%,#0F3D3A 0%,transparent 60%),
            radial-gradient(900px 600px at 100% 100%,#3a0d13 0%,transparent 60%),#06191a}
            .bg i{position:absolute;border-radius:50%;filter:blur(90px);animation:float 18s ease-in-out infinite}
            .bg i:nth-child(1){width:480px;height:480px;background:var(--wine);opacity:.55;top:-120px;right:8%}
            .bg i:nth-child(2){width:520px;height:520px;background:var(--teal-l);opacity:.25;bottom:-160px;left:18%;animation-delay:-6s}
            .bg i:nth-child(3){width:380px;height:380px;background:var(--blue);opacity:.42;top:40%;left:55%;animation-delay:-12s}
            .bg::after{content:'';position:absolute;inset:0;background-image:linear-gradient(rgba(255,255,255,.035) 1px,transparent 1px),
            linear-gradient(90deg,rgba(255,255,255,.035) 1px,transparent 1px);background-size:48px 48px;
            -webkit-mask-image:radial-gradient(circle at 50% 30%,#000,transparent 75%);mask-image:radial-gradient(circle at 50% 30%,#000,transparent 75%)}
            .glow{position:fixed;width:460px;height:460px;border-radius:50%;pointer-events:none;z-index:-1;left:-999px;top:-999px;
            background:radial-gradient(circle,rgba(63,93,171,.25),transparent 65%);transform:translate(-50%,-50%)}

            /* sidebar */
            aside{position:fixed;top:0;left:0;bottom:0;width:240px;z-index:30;overflow:auto;padding-bottom:20px;
            background:rgba(8,32,31,.55);backdrop-filter:blur(24px) saturate(170%);-webkit-backdrop-filter:blur(24px) saturate(170%);
            border-right:1px solid var(--glass-b);box-shadow:10px 0 40px rgba(0,0,0,.25)}
            .logo{display:flex;align-items:center;gap:12px;padding:24px 22px 18px;font-size:20px;font-weight:800;letter-spacing:.5px}
            .logo small{display:block;font-size:10px;font-weight:500;color:var(--mute);letter-spacing:1.6px;text-transform:uppercase}
            .mark{display:inline-grid;place-items:center;width:38px;height:38px;border-radius:12px;font-weight:800;color:#fff;
            background:linear-gradient(135deg,var(--wine),var(--blue));animation:pulse 2.6s infinite;border:1px solid rgba(255,255,255,.25)}
            .mark.big{width:58px;height:58px;font-size:26px;border-radius:18px;margin-bottom:14px}
            aside a{display:flex;align-items:center;gap:12px;margin:4px 14px;padding:12px 14px;border-radius:12px;color:var(--mute);
            text-decoration:none;font-size:14px;font-weight:500;transition:all .25s ease;animation:slideIn .55s cubic-bezier(.2,.8,.2,1) backwards;
            animation-delay:calc(var(--n,0)*55ms + 100ms)}
            aside a em{font-style:normal;width:22px;text-align:center;transition:transform .25s}
            aside a:hover{background:var(--glass);color:#fff;transform:translateX(5px)}aside a:hover em{transform:scale(0.75) rotate(-8deg)}
            aside a.on{color:#fff;background:linear-gradient(135deg,rgba(63,93,171,.9),rgba(15,61,58,.95));
            box-shadow:0 10px 26px rgba(63,93,171,.38),inset 0 0 0 1px rgba(255,255,255,.2)}
            aside a.out{margin-top:22px;color:#ffb3bc}aside a.out:hover{background:rgba(108,21,30,.55);color:#fff}

            /* header + layout */
            main{margin-left:240px;position:relative}
            header{position:sticky;top:0;z-index:20;display:flex;justify-content:space-between;align-items:center;padding:16px 32px;
            background:rgba(8,32,31,.5);backdrop-filter:blur(24px) saturate(170%);-webkit-backdrop-filter:blur(24px) saturate(170%);
            border-bottom:1px solid var(--glass-b)}
            header b{font-weight:700;letter-spacing:.3px}
            .pill{padding:7px 16px;border-radius:30px;background:var(--glass);border:1px solid var(--glass-b);color:var(--mute);font-size:13px}
            .pill b{color:#fff;font-variant-numeric:tabular-nums}
            section{padding:32px}

            /* typography */
            h2{margin:0 0 8px;font-size:30px;font-weight:800;letter-spacing:-.6px;background:linear-gradient(90deg,#fff,#9fb4f0 60%,#e7a3ac);
            -webkit-background-clip:text;background-clip:text;color:transparent}
            h3{margin:0 0 14px;font-size:16px;font-weight:700;color:#dfe9ff}
            .sub{margin:0 0 6px;color:var(--mute)}
            p{line-height:1.6}
            .total{font-size:17px;margin-top:18px}.total b{color:#9fb4f0}

            /* glass surfaces */
            .cards{display:flex;flex-wrap:wrap;gap:20px;margin:22px 0}
            .card{position:relative;overflow:hidden;width:215px;padding:22px;border-radius:20px;text-decoration:none;color:inherit;
            background:var(--glass);backdrop-filter:blur(20px) saturate(160%);-webkit-backdrop-filter:blur(20px) saturate(160%);
            border:1px solid var(--glass-b);box-shadow:0 18px 40px rgba(0,0,0,.28),inset 0 1px 0 rgba(255,255,255,.14);
            transition:transform .25s ease-out,box-shadow .3s;will-change:transform}
            .card::after{content:'';position:absolute;left:0;top:0;width:100%;height:3px;background:linear-gradient(90deg,var(--wine-l),var(--teal-l),var(--blue-l))}
            .card:hover{box-shadow:0 28px 60px rgba(0,0,0,.4),0 0 40px rgba(63,93,171,.25),inset 0 1px 0 rgba(255,255,255,.2)}
            .card h3{margin:0;font-size:11px;color:var(--mute);text-transform:uppercase;letter-spacing:1px}
            .card p{margin:12px 0 0;font-size:28px;font-weight:800;line-height:0.7;background:linear-gradient(135deg,#fff,#b9c9ff);
            -webkit-background-clip:text;background-clip:text;color:transparent}
            .card::before,.panel::before{content:'';position:absolute;inset:0;border-radius:inherit;pointer-events:none;opacity:0;transition:opacity .3s;
            background:radial-gradient(280px circle at var(--mx,50%) var(--my,-50%),rgba(255,255,255,.14),transparent 60%)}
            .card:hover::before,.panel:hover::before{opacity:1}
            .panel{position:relative;padding:24px;margin:18px 0;border-radius:20px;background:var(--glass);
            backdrop-filter:blur(22px) saturate(160%);-webkit-backdrop-filter:blur(22px) saturate(160%);border:1px solid var(--glass-b);
            box-shadow:0 20px 50px rgba(0,0,0,.28),inset 0 1px 0 rgba(255,255,255,.12);transition:box-shadow .3s}
            .panel:hover{box-shadow:0 28px 64px rgba(0,0,0,.38),inset 0 1px 0 rgba(255,255,255,.18)}
            .grid2{display:grid;grid-template-columns:1fr 1fr;gap:20px}.grid2 .panel{margin:18px 0}

            /* tables */
            table{width:100%;border-collapse:separate;border-spacing:0;background:var(--glass);backdrop-filter:blur(18px) saturate(150%);
            -webkit-backdrop-filter:blur(18px) saturate(150%);border:1px solid var(--glass-b);border-radius:16px;overflow:hidden;
            box-shadow:0 20px 50px rgba(0,0,0,.25)}
            th,td{padding:13px 16px;text-align:left;font-size:14px;border-bottom:1px solid rgba(255,255,255,.07)}
            th{background:linear-gradient(90deg,rgba(108,21,30,.9),rgba(15,61,58,.95),rgba(63,93,171,.9));color:#fff;font-weight:600;
            font-size:12px;letter-spacing:.6px;text-transform:uppercase}
            td{transition:background .2s}
            tr:not(:first-child):hover td{background:rgba(255,255,255,.07)}
            tr:last-child td{border-bottom:0}
            .kv th{background:rgba(255,255,255,.05);color:var(--mute);width:210px;text-transform:none;font-size:13px;letter-spacing:0}
            .who{display:flex;align-items:center;gap:11px}
            .av{width:34px;height:34px;border-radius:50%;display:grid;place-items:center;font-size:12px;font-weight:700;color:#fff;
            border:1px solid rgba(255,255,255,.28);box-shadow:0 4px 12px rgba(0,0,0,.35)}
            .a0{background:linear-gradient(135deg,var(--wine),var(--blue))}.a1{background:linear-gradient(135deg,var(--teal-l),var(--blue))}
            .a2{background:linear-gradient(135deg,var(--wine-l),var(--teal))}

            /* buttons */
            .btn,.sm{position:relative;overflow:hidden;display:inline-block;color:#fff;border:1px solid rgba(255,255,255,.2);cursor:pointer;
            text-decoration:none;font-family:inherit;font-weight:600;transition:transform .2s,box-shadow .25s,filter .2s}
            .btn{padding:10px 20px;border-radius:12px;font-size:14px;background:linear-gradient(135deg,var(--blue),#2f4a8e);box-shadow:0 8px 22px rgba(63,93,171,.38)}
            .btn:hover{transform:translateY(-2px);box-shadow:0 14px 30px rgba(63,93,171,.55);filter:brightness(1.12)}
            .btn:active,.sm:active{transform:translateY(0) scale(.96)}
            .btn.dark{background:linear-gradient(135deg,rgba(255,255,255,.16),rgba(255,255,255,.05));box-shadow:none;backdrop-filter:blur(10px)}
            .btn.dark:hover{box-shadow:0 10px 24px rgba(0,0,0,.35)}
            .btn.green,.sm.green{background:linear-gradient(135deg,#1b8077,var(--teal));box-shadow:0 8px 22px rgba(15,61,58,.6)}
            .sm{padding:6px 12px;border-radius:8px;font-size:12px;margin-right:5px;background:linear-gradient(135deg,var(--blue),#2f4a8e)}
            .sm:hover{transform:translateY(-2px);filter:brightness(1.15)}
            .sm.red{background:linear-gradient(135deg,#8c1d29,var(--wine))}
            .rip{position:absolute;border-radius:50%;background:rgba(255,255,255,.45);transform:scale(0);animation:ripple .65s ease-out;pointer-events:none}
            .act{white-space:nowrap}.act form{display:inline}

            /* forms */
            input,select{width:100%;padding:11px 14px;margin-top:6px;font-size:14px;font-family:inherit;color:var(--ink);outline:0;
            background:rgba(255,255,255,.07);border:1px solid var(--glass-b);border-radius:12px;
            transition:border-color .2s,box-shadow .25s,background .2s}
            input:focus,select:focus{border-color:var(--blue-l);background:rgba(255,255,255,.11);box-shadow:0 0 0 4px rgba(63,93,171,.3)}
            input::placeholder{color:#7f9693}select option{background:#0F3D3A;color:#fff}input[type=date]{color-scheme:dark}
            label{display:block;margin-top:14px;font-size:12px;font-weight:600;color:var(--mute);letter-spacing:.6px;text-transform:uppercase}
            .bar2{display:flex;flex-wrap:wrap;gap:10px;margin:18px 0;align-items:center}.bar2 input,.bar2 select{width:260px;margin:0}
            .bar2 form{display:inline}
            .formgrid{display:grid;grid-template-columns:1fr 1fr;gap:4px 22px}.formgrid .full{grid-column:1/3;margin-top:18px}

            /* badges */
            .badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:12px;font-weight:700;border:1px solid transparent}
            .b-ok{background:rgba(47,168,156,.18);color:#72e3d6;border-color:rgba(47,168,156,.45)}
            .b-bad{background:rgba(184,65,79,.22);color:#ffa0ab;border-color:rgba(184,65,79,.5)}
            .b-wait{background:rgba(63,93,171,.28);color:#aebfF5;border-color:rgba(125,152,224,.5)}
            .b-info{background:rgba(255,255,255,.1);color:#dbe5e4;border-color:rgba(255,255,255,.22)}

            /* charts */
            .bar{display:flex;align-items:center;gap:12px;margin:13px 0}.bar span{width:150px;font-size:13px;color:var(--mute)}
            .bar div{flex:1;height:12px;border-radius:8px;background:rgba(255,255,255,.08);overflow:hidden}
            .bar i{display:block;height:100%;width:0;border-radius:8px;background:linear-gradient(90deg,var(--wine-l),var(--blue-l));
            box-shadow:0 0 14px rgba(125,152,224,.6);transition:width 1.3s cubic-bezier(.2,.8,.2,1)}
            .bar b{width:26px;text-align:right}
            .donut-wrap{display:flex;align-items:center;gap:30px;flex-wrap:wrap}
            .donut{width:178px;height:178px;border-radius:50%;position:relative;display:grid;place-items:center;
            box-shadow:0 0 44px rgba(63,93,171,.4);animation:spinIn 1.3s cubic-bezier(.2,.8,.2,1) .25s backwards}
            .donut::before{content:'';position:absolute;inset:26px;border-radius:50%;background:#0b2b2a;box-shadow:inset 0 0 22px rgba(0,0,0,.6)}
            .dc{position:relative;text-align:center}.dc b{display:block;font-size:30px;font-weight:800}.dc small{color:var(--mute);font-size:12px}
            .legend p{display:flex;align-items:center;gap:10px;margin:9px 0;color:var(--mute);font-size:14px}
            .legend i{display:inline-block;width:12px;height:12px;border-radius:4px}.legend b{color:#fff;margin-left:4px}

            /* skeleton loader */
            .hide{display:none}
            .sk{position:relative;overflow:hidden;background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.08);border-radius:12px}
            .sk::after{content:'';position:absolute;inset:0;transform:translateX(-100%);
            background:linear-gradient(90deg,transparent,rgba(125,152,224,.22),rgba(255,255,255,.18),transparent);animation:shimmer 1.4s infinite}
            .sk-title{height:34px;width:280px;margin-bottom:12px}.sk-sub{height:14px;width:200px;margin-bottom:26px}
            .sk-cards{display:flex;flex-wrap:wrap;gap:20px;margin-bottom:22px}.sk-card{width:215px;height:108px;border-radius:20px}
            .sk-panel{height:200px;border-radius:20px;margin-bottom:22px}.sk-row{height:46px;margin-bottom:10px;border-radius:10px}
            #skel{animation:fade .3s}

            /* toast + modal */
            .flash{position:fixed;top:84px;right:28px;z-index:60;max-width:360px;padding:14px 20px;border-radius:16px;font-size:14px;font-weight:500;
            background:linear-gradient(135deg,rgba(15,61,58,.88),rgba(63,93,171,.6));border:1px solid var(--glass-b);
            backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);box-shadow:0 18px 44px rgba(0,0,0,.45);
            animation:toastIn .7s cubic-bezier(.2,1.2,.4,1) 1.3s backwards;transition:opacity .4s,transform .4s}
            .flash::before{content:'✔ ';color:#72e3d6}.flash.gone{opacity:0;transform:translateX(50px);pointer-events:none}
            .modal{position:fixed;inset:0;z-index:100;display:grid;place-items:center;background:rgba(3,12,12,.55);
            backdrop-filter:blur(8px);-webkit-backdrop-filter:blur(8px);opacity:0;transition:opacity .25s}.modal.in{opacity:1}
            .mbox{width:min(390px,90vw);padding:32px;border-radius:24px;text-align:center;
            background:linear-gradient(145deg,rgba(255,255,255,.15),rgba(255,255,255,.05));border:1px solid var(--glass-b);
            box-shadow:0 30px 80px rgba(0,0,0,.6);transform:scale(.85) translateY(20px);transition:transform .35s cubic-bezier(.2,1.3,.4,1)}
            .modal.in .mbox{transform:none}
            .mic{width:58px;height:58px;margin:0 auto 14px;border-radius:50%;display:grid;place-items:center;font-size:26px;font-weight:800;
            background:linear-gradient(135deg,var(--wine),var(--blue));box-shadow:0 0 34px rgba(108,21,30,.75)}
            .mbox p{margin:0;font-size:16px}.mact{display:flex;gap:10px;justify-content:center;margin-top:22px}

            /* reveal choreography */
            .reveal h2,.reveal .sub,.reveal .card,.reveal .panel,.reveal .bar2,.reveal .total{
            animation:up .7s cubic-bezier(.2,.8,.2,1) backwards;animation-delay:calc(var(--i,0)*70ms)}
            .reveal tr{animation:rowIn .5s ease backwards;animation-delay:calc(var(--r,0)*35ms + 200ms)}

            /* login */
            .lg{display:flex;align-items:center;justify-content:center;padding:20px}
            .login{position:relative;width:390px;max-width:100%;padding:42px 38px;border-radius:28px;
            background:linear-gradient(145deg,rgba(255,255,255,.14),rgba(255,255,255,.05));backdrop-filter:blur(28px) saturate(170%);
            -webkit-backdrop-filter:blur(28px) saturate(170%);border:1px solid var(--glass-b);
            box-shadow:0 40px 90px rgba(0,0,0,.55),inset 0 1px 0 rgba(255,255,255,.25);animation:up .9s cubic-bezier(.2,.8,.2,1) backwards}
            .login.shake{animation:up .9s cubic-bezier(.2,.8,.2,1) backwards,shake .5s .9s}
            .login h1{margin:0 0 6px;font-size:30px;font-weight:800;background:linear-gradient(90deg,#fff,#9fb4f0 60%,#e7a3ac);
            -webkit-background-clip:text;background-clip:text;color:transparent}
            .login p{margin:0 0 8px;color:var(--mute);font-size:14px}
            .login .btn{width:100%;margin-top:26px;padding:14px;font-size:15px}
            .err{background:rgba(108,21,30,.6);border:1px solid rgba(184,65,79,.7);color:#ffd5da;padding:11px 14px;border-radius:12px;margin:14px 0 4px;font-size:14px}
            .hint{margin-top:18px;text-align:center;font-size:12px;color:var(--mute)}

            /* keyframes */
            @keyframes float{0%,100%{transform:translate(0,0) scale(1)}33%{transform:translate(60px,-40px) scale(1.12)}66%{transform:translate(-50px,50px) scale(.92)}}
            @keyframes up{from{opacity:0;transform:translateY(26px) scale(.98)}to{opacity:1;transform:none}}
            @keyframes rowIn{from{opacity:0;transform:translateX(-16px)}to{opacity:1;transform:none}}
            @keyframes slideIn{from{opacity:0;transform:translateX(-26px)}to{opacity:1;transform:none}}
            @keyframes toastIn{from{opacity:0;transform:translateX(70px) scale(.9)}to{opacity:1;transform:none}}
            @keyframes spinIn{from{opacity:0;transform:rotate(-140deg) scale(.4)}to{opacity:1;transform:none}}
            @keyframes shimmer{to{transform:translateX(100%)}}
            @keyframes ripple{to{transform:scale(2.6);opacity:0}}
            @keyframes fade{from{opacity:0}to{opacity:1}}
            @keyframes pulse{0%,100%{box-shadow:0 0 0 0 rgba(63,93,171,.65)}50%{box-shadow:0 0 0 14px rgba(63,93,171,0)}}
            @keyframes shake{0%,100%{transform:none}20%{transform:translateX(-10px)}40%{transform:translateX(9px)}60%{transform:translateX(-6px)}80%{transform:translateX(4px)}}

            @media(max-width:900px){aside{position:static;width:auto}main{margin:0}.grid2,.formgrid{grid-template-columns:1fr}
            .formgrid .full{grid-column:1}.flash{top:12px;right:12px}.bar2 input,.bar2 select{width:100%}section{padding:18px}
            table{display:block;overflow-x:auto}header{position:static}}
            @media(prefers-reduced-motion:reduce){*,*::before,*::after{animation-duration:.01ms!important;animation-delay:0s!important;transition-duration:.01ms!important}}
            @media print{.bg,.glow,aside,header,.bar2,.flash{display:none}main{margin:0}body{background:#fff;color:#000}
            .panel,table,.card{background:#fff;color:#000;box-shadow:none;backdrop-filter:none;border-color:#ccc}
            th{background:#ddd;color:#000}h2,.card p{background:none;color:#000;-webkit-text-fill-color:#000}td{color:#000}}
            """;

    static final String SCRIPT = """
            (function(){
            var sk=document.getElementById('skel'),ct=document.getElementById('content'),glow=document.getElementById('glow');

            function stagger(){
              var i=0;ct.querySelectorAll('h2,.sub,.card,.panel,.bar2,.total').forEach(function(el){el.style.setProperty('--i',Math.min(i++,14))});
              var r=0;ct.querySelectorAll('tr').forEach(function(el){el.style.setProperty('--r',Math.min(r++,22))});
            }
            function counters(){
              ct.querySelectorAll('.card p').forEach(function(p){
                var t=p.textContent.trim(),a=t.search(/[0-9]/);if(a<0)return;
                var b=t.length-1;while(b>=0&&!/[0-9]/.test(t[b]))b--;
                var pre=t.slice(0,a),suf=t.slice(b+1),n=parseFloat(t.slice(a,b+1).replace(/,/g,''));
                if(isNaN(n))return;
                var s=performance.now(),d=1200;
                (function f(now){var k=Math.min(1,(now-s)/d),e=1-Math.pow(1-k,4);
                  p.textContent=pre+Math.round(n*e).toLocaleString('en-US')+suf;if(k<1)requestAnimationFrame(f)})(s);
              });
            }
            function bars(){setTimeout(function(){ct.querySelectorAll('.bar i').forEach(function(i){i.style.width=i.getAttribute('data-w')+'%'})},250)}
            function reveal(){
              if(sk)sk.remove();
              if(!ct)return;
              ct.classList.remove('hide');ct.classList.add('reveal');
              stagger();counters();bars();
            }
            setTimeout(reveal,1200);

            /* cursor glow, spotlight, 3D tilt */
            document.addEventListener('mousemove',function(e){
              if(glow){glow.style.left=e.clientX+'px';glow.style.top=e.clientY+'px'}
              var c=e.target.closest&&e.target.closest('.card,.panel');
              if(c){
                var r=c.getBoundingClientRect();
                c.style.setProperty('--mx',(e.clientX-r.left)+'px');c.style.setProperty('--my',(e.clientY-r.top)+'px');
                if(c.classList.contains('card')){
                  var px=(e.clientX-r.left)/r.width-.5,py=(e.clientY-r.top)/r.height-.5;
                  c.style.transform='perspective(700px) rotateX('+(-py*10)+'deg) rotateY('+(px*12)+'deg) translateY(-5px)';
                }
              }
            });
            document.addEventListener('mouseout',function(e){
              var c=e.target.closest&&e.target.closest('.card');
              if(c&&!c.contains(e.relatedTarget))c.style.transform='';
            });

            /* button ripple */
            document.addEventListener('click',function(e){
              var b=e.target.closest&&e.target.closest('.btn,.sm');if(!b)return;
              var r=b.getBoundingClientRect(),s=document.createElement('span'),d=Math.max(r.width,r.height);
              s.className='rip';
              s.style.cssText='width:'+d+'px;height:'+d+'px;left:'+(e.clientX-r.left-d/2)+'px;top:'+(e.clientY-r.top-d/2)+'px';
              b.appendChild(s);setTimeout(function(){s.remove()},650);
            });

            /* smooth page transitions */
            document.addEventListener('click',function(e){
              var a=e.target.closest&&e.target.closest('a[href]');
              if(!a||e.ctrlKey||e.metaKey||e.shiftKey||a.target)return;
              var h=a.getAttribute('href');
              if(!h||h.charAt(0)!=='/'||h.indexOf('export.csv')>-1)return;
              e.preventDefault();document.body.classList.add('leaving');
              setTimeout(function(){location.href=h},260);
            });

            /* glass confirm dialog */
            function ask(msg,cb){
              var o=document.createElement('div');o.className='modal';
              o.innerHTML="<div class='mbox'><div class='mic'>?</div><p></p><div class='mact'><button class='btn dark' data-x='0'>Cancel</button><button class='btn' data-x='1'>Confirm</button></div></div>";
              o.querySelector('p').textContent=msg;document.body.appendChild(o);
              requestAnimationFrame(function(){o.classList.add('in')});
              o.addEventListener('click',function(e){
                var x=e.target.getAttribute&&e.target.getAttribute('data-x');
                if(x===null&&e.target!==o)return;
                o.classList.remove('in');setTimeout(function(){o.remove()},250);
                if(x==='1')cb();
              });
            }
            document.addEventListener('submit',function(e){
              var f=e.target;
              if(f.getAttribute&&f.getAttribute('data-confirm')&&!f.getAttribute('data-ok')){
                e.preventDefault();
                ask(f.getAttribute('data-confirm'),function(){f.setAttribute('data-ok','1');f.submit()});
              }
            });

            /* toast auto-dismiss + live clock */
            var fl=document.querySelector('.flash');if(fl)setTimeout(function(){fl.classList.add('gone')},5500);
            var ck=document.getElementById('clock');
            function tick(){if(ck)ck.textContent=new Date().toLocaleTimeString()}tick();setInterval(tick,1000);
            window.addEventListener('pageshow',function(){document.body.classList.remove('leaving')});
            })();
            """;

    static String card(String t, String v, String href) {
        return "<a class='card' href='" + href + "'><h3>" + esc(t) + "</h3><p>" + esc(v) + "</p></a>";
    }

    static String badge(String s) {
        String c = switch (s) {
            case "Active", "Present", "Approved", "Paid" -> "b-ok";
            case "Absent", "Rejected", "Inactive" -> "b-bad";
            case "Pending", "Not Marked" -> "b-wait";
            default -> "b-info";
        };
        return "<span class='badge " + c + "'>" + esc(s) + "</span>";
    }

    static String avatar(String n) {
        String[] w = n.trim().split("\\s+");
        String s = w[0].substring(0, 1) + (w.length > 1 ? w[w.length - 1].substring(0, 1) : "");
        return "<span class='av a" + (Math.abs(n.hashCode()) % 3) + "'>" + esc(s.toUpperCase()) + "</span>";
    }

    static String post(String action, String cls, String label, String confirm, String... kv) {
        StringBuilder sb = new StringBuilder("<form method='post' action='" + action + "'"
                + (confirm != null ? " data-confirm='" + esc(confirm) + "'" : "") + ">");
        for (int i = 0; i + 1 < kv.length; i += 2)
            sb.append("<input type='hidden' name='").append(kv[i]).append("' value='").append(esc(kv[i + 1])).append("'>");
        return sb.append("<button class='").append(cls).append("'>").append(esc(label)).append("</button></form>").toString();
    }

    static String inp(String label, String name, String val, String type) {
        return "<div><label>" + label + "</label><input type='" + type + "' name='" + name + "' value='" + esc(val) + "'"
                + (name.equals("name") || name.equals("salary") ? " required" : "") + "></div>";
    }

    static String kv(String k, String v) { return "<tr><th>" + esc(k) + "</th><td>" + esc(v) + "</td></tr>"; }

    static double net(Emp e) { return e.salary * 1.08; }

    static String nm(String id) { Emp e = emps.get(id); return e == null ? id : e.name; }

    static String money(double d) { return String.format(Locale.US, "₹%,.0f", d); }

    static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static Map<String, String> parse(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.isEmpty()) return m;
        for (String kv : s.split("&")) {
            String[] p = kv.split("=", 2);
            m.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    static String sid(HttpExchange x) {
        String c = x.getRequestHeaders().getFirst("Cookie");
        if (c == null) return "";
        for (String part : c.split(";")) if (part.trim().startsWith("SID=")) return part.trim().substring(4);
        return "";
    }

    static boolean loggedIn(HttpExchange x) { return sessions.contains(sid(x)); }

    static void redirect(HttpExchange x, String to) throws IOException {
        x.getResponseHeaders().set("Location", to);
        x.sendResponseHeaders(302, -1);
        x.close();
    }

    static void send(HttpExchange x, String body, String type) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        x.sendResponseHeaders(200, b.length);
        x.getResponseBody().write(b);
        x.close();
    }
}