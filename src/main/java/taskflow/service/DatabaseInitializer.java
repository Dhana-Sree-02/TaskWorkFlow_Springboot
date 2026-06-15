package taskflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Starting Database Seeding and Initialization...");

            // 1. Seed Roles table
            jdbcTemplate.execute("INSERT INTO roles (role, rolename) VALUES (1, 'User') ON CONFLICT (role) DO UPDATE SET rolename = EXCLUDED.rolename");
            jdbcTemplate.execute("INSERT INTO roles (role, rolename) VALUES (2, 'Admin') ON CONFLICT (role) DO UPDATE SET rolename = EXCLUDED.rolename");
            jdbcTemplate.execute("INSERT INTO roles (role, rolename) VALUES (3, 'Manager') ON CONFLICT (role) DO UPDATE SET rolename = EXCLUDED.rolename");

            // 2. Seed Menus (mid 1 = Dashboard, mid 2 = My Tasks, mid 3 = Semantic Search, mid 4 = User Manager, mid 5 = Profile)
            jdbcTemplate.execute("INSERT INTO menus (mid, menu, icon) VALUES (1, 'Dashboard', 'dashboard.png') ON CONFLICT (mid) DO UPDATE SET menu = EXCLUDED.menu, icon = EXCLUDED.icon");
            jdbcTemplate.execute("INSERT INTO menus (mid, menu, icon) VALUES (2, 'My Tasks', 'tasks.png') ON CONFLICT (mid) DO UPDATE SET menu = EXCLUDED.menu, icon = EXCLUDED.icon");
            jdbcTemplate.execute("INSERT INTO menus (mid, menu, icon) VALUES (3, 'Semantic Search', 'search.png') ON CONFLICT (mid) DO UPDATE SET menu = EXCLUDED.menu, icon = EXCLUDED.icon");
            jdbcTemplate.execute("INSERT INTO menus (mid, menu, icon) VALUES (4, 'User Manager', 'users.png') ON CONFLICT (mid) DO UPDATE SET menu = EXCLUDED.menu, icon = EXCLUDED.icon");
            jdbcTemplate.execute("INSERT INTO menus (mid, menu, icon) VALUES (5, 'Profile', 'profile.png') ON CONFLICT (mid) DO UPDATE SET menu = EXCLUDED.menu, icon = EXCLUDED.icon");

            // 3. Seed Role Mappings for Role 1 (User) -> mid 1, 2, 3, 5
            jdbcTemplate.execute("DELETE FROM roles_mapping WHERE role = 1");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (1, 1)");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (1, 2)");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (1, 3)");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (1, 5)");

            // 4. Seed Role Mappings for Role 2 (Administrator) -> mid 1, 2, 3, 4, 5
            jdbcTemplate.execute("DELETE FROM roles_mapping WHERE role = 2");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (2, 1)");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (2, 2)");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (2, 3)");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (2, 4)");
            jdbcTemplate.execute("INSERT INTO roles_mapping (role, mid) VALUES (2, 5)");

            // 6. Seed a default Administrator account if it does not already exist
            Integer adminCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'admin@taskworkflow.com'", 
                Integer.class
            );
            if (adminCount == null || adminCount == 0) {
                jdbcTemplate.execute(
                    "INSERT INTO users (fullname, phone, email, password, role, status) " +
                    "VALUES ('Administrator', '+1 555-0199', 'admin@taskworkflow.com', 'admin', 2, 1)"
                );
                System.out.println("Default Administrator account (admin@taskworkflow.com / admin) seeded successfully.");
            }

            System.out.println("--- Database successfully initialized & seeded with Admin / User configuration ---");
        } catch (Exception e) {
            System.err.println("Note during Database Initializer execution (e.g. database schema is generating, will retry on next startup): " + e.getMessage());
        }
    }
}
