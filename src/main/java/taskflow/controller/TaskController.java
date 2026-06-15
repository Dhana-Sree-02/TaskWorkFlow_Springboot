package taskflow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import taskflow.service.TaskService;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/tasks")
    public List<Map<String, Object>> getTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", defaultValue = "1") int role) {
        return taskService.getTasks(userId, role);
    }

    @PostMapping("/tasks")
    public Map<String, Object> createTask(@RequestBody Map<String, Object> payload) {
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        String priority = (String) payload.get("priority");
        String dueDate = (String) payload.get("due_date");
        Long assignedUserId = payload.get("assigned_user_id") != null ? Long.valueOf(payload.get("assigned_user_id").toString()) : null;

        return taskService.createTask(title, description, priority, dueDate, assignedUserId);
    }

    @PutMapping("/tasks/{id}")
    public Map<String, Object> updateTask(@PathVariable("id") Long id, @RequestBody Map<String, Object> payload) {
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        String priority = (String) payload.get("priority");
        String dueDate = (String) payload.get("due_date");

        return taskService.updateTask(id, title, description, priority, dueDate);
    }

    @DeleteMapping("/tasks/{id}")
    public Map<String, Object> deleteTask(@PathVariable("id") Long id) {
        return taskService.deleteTask(id);
    }

    @PutMapping("/tasks/{id}/status")
    public Map<String, Object> updateTaskStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-User-Name", defaultValue = "System") String userName) {
        String status = (String) payload.get("status");
        return taskService.updateTaskStatus(id, status, userName);
    }

    @PostMapping("/assign-task")
    public Map<String, Object> assignTask(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-User-Name", defaultValue = "Manager") String managerName) {
        Long taskId = Long.valueOf(payload.get("task_id").toString());
        Long userId = Long.valueOf(payload.get("user_id").toString());

        return taskService.assignTask(taskId, userId, managerName);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {
        return taskService.getDashboardAnalytics();
    }
}
