package taskflow.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import taskflow.model.*;
import taskflow.repository.*;

import java.time.LocalDate;
import java.util.*;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private UsersRepository usersRepository;

    public List<Map<String, Object>> getTasks(Long userId, int role) {
        List<Task> tasks;
        if (role == 1) { // Employee
            tasks = taskRepository.findTasksAssignedToUser(userId);
        } else { // Manager (3) or Admin (2)
            tasks = taskRepository.findAll();
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (Task t : tasks) {
            Map<String, Object> tMap = new HashMap<>();
            tMap.put("task_id", t.getTaskId());
            tMap.put("title", t.getTitle());
            tMap.put("description", t.getDescription());
            tMap.put("priority", t.getPriority());
            tMap.put("due_date", t.getDueDate() != null ? t.getDueDate().toString() : null);
            tMap.put("created_at", t.getCreatedAt().toString());

            // Status stage
            Optional<TaskStatus> statusOpt = taskStatusRepository.findByTaskTaskId(t.getTaskId());
            String stage = statusOpt.isPresent() ? statusOpt.get().getCurrentStage() : "Backlog";
            tMap.put("current_stage", stage);

            // Assignment
            Optional<Assignment> assignOpt = assignmentRepository.findByTaskTaskId(t.getTaskId());
            if (assignOpt.isPresent()) {
                Users u = assignOpt.get().getUser();
                Map<String, Object> assignMap = new HashMap<>();
                assignMap.put("user_id", u.getId());
                assignMap.put("fullname", u.getFullname());
                assignMap.put("email", u.getEmail());
                tMap.put("assigned_to", assignMap);
            } else {
                tMap.put("assigned_to", null);
            }

            response.add(tMap);
        }
        return response;
    }

    @Transactional
    public Map<String, Object> createTask(String title, String description, String priority, String dueDateStr, Long assignedUserId) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority != null ? priority : "Medium");
        if (dueDateStr != null && !dueDateStr.isEmpty()) {
            task.setDueDate(LocalDate.parse(dueDateStr));
        }

        task = taskRepository.save(task);

        // Save status
        TaskStatus status = new TaskStatus();
        status.setTask(task);
        status.setCurrentStage("Backlog");
        taskStatusRepository.save(status);

        String assignedUserName = "Unassigned";
        if (assignedUserId != null) {
            Optional<Users> userOpt = usersRepository.findById(assignedUserId);
            if (userOpt.isPresent()) {
                Assignment assignment = new Assignment();
                assignment.setTask(task);
                assignment.setUser(userOpt.get());
                assignmentRepository.save(assignment);
                assignedUserName = userOpt.get().getFullname();
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task created successfully.");
        res.put("task_id", task.getTaskId());
        res.put("assigned_user_name", assignedUserName);
        return res;
    }

    @Transactional
    public Map<String, Object> updateTask(Long taskId, String title, String description, String priority, String dueDateStr) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found");
        }

        Task task = taskOpt.get();
        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (priority != null) task.setPriority(priority);
        if (dueDateStr != null) {
            task.setDueDate(dueDateStr.isEmpty() ? null : LocalDate.parse(dueDateStr));
        }

        taskRepository.save(task);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task updated successfully.");
        return res;
    }

    @Transactional
    public Map<String, Object> deleteTask(Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found");
        }

        // Clean status and assignments first manually or let JPA handle it
        assignmentRepository.deleteByTaskTaskId(taskId);
        taskStatusRepository.deleteByTaskTaskId(taskId);
        taskRepository.deleteById(taskId);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task deleted successfully.");
        return res;
    }

    @Transactional
    public Map<String, Object> updateTaskStatus(Long taskId, String targetStage, String updatedByFullname) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found");
        }

        TaskStatus statusRecord = taskStatusRepository.findByTaskTaskId(taskId)
                .orElseGet(() -> {
                    TaskStatus status = new TaskStatus();
                    status.setTask(taskOpt.get());
                    status.setCurrentStage("Backlog");
                    return status;
                });

        String currentStage = statusRecord.getCurrentStage();
        if (!isValidTransition(currentStage, targetStage)) {
            throw new IllegalArgumentException("Invalid transition from '" + currentStage + "' to '" + targetStage + "'.");
        }

        statusRecord.setCurrentStage(targetStage);
        taskStatusRepository.save(statusRecord);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task status updated to '" + targetStage + "'.");
        res.put("task_id", taskId);
        res.put("current_stage", targetStage);
        res.put("old_stage", currentStage);
        return res;
    }

    @Transactional
    public Map<String, Object> assignTask(Long taskId, Long userId, String managerName) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found");
        }

        Optional<Users> userOpt = usersRepository.findById(userId);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("Target user not found");
        }

        Optional<Assignment> existingAssignment = assignmentRepository.findByTaskTaskId(taskId);
        String oldAssignee = "Unassigned";

        if (existingAssignment.isPresent()) {
            Assignment ass = existingAssignment.get();
            oldAssignee = ass.getUser().getFullname();
            ass.setUser(userOpt.get());
            assignmentRepository.save(ass);
        } else {
            Assignment ass = new Assignment();
            ass.setTask(taskOpt.get());
            ass.setUser(userOpt.get());
            assignmentRepository.save(ass);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task successfully assigned to '" + userOpt.get().getFullname() + "'.");
        res.put("old_assignee", oldAssignee);
        res.put("new_assignee", userOpt.get().getFullname());
        return res;
    }

    public Map<String, Object> getDashboardAnalytics() {
        List<Task> allTasks = taskRepository.findAll();
        int total = allTasks.size();
        int completed = 0;
        int pending = 0;
        int highPriority = 0;

        Map<String, Integer> stageCounts = new LinkedHashMap<>();
        stageCounts.put("Backlog", 0);
        stageCounts.put("To Do", 0);
        stageCounts.put("In Progress", 0);
        stageCounts.put("Review", 0);
        stageCounts.put("Completed", 0);

        Map<String, Integer> userTaskCounts = new HashMap<>();

        for (Task t : allTasks) {
            Optional<TaskStatus> statusOpt = taskStatusRepository.findByTaskTaskId(t.getTaskId());
            String stage = statusOpt.isPresent() ? statusOpt.get().getCurrentStage() : "Backlog";
            stageCounts.put(stage, stageCounts.getOrDefault(stage, 0) + 1);

            if ("Completed".equals(stage)) {
                completed++;
            } else {
                pending++;
                if ("High".equalsIgnoreCase(t.getPriority())) {
                    highPriority++;
                }
            }

            Optional<Assignment> assignOpt = assignmentRepository.findByTaskTaskId(t.getTaskId());
            if (assignOpt.isPresent()) {
                String fullname = assignOpt.get().getUser().getFullname();
                userTaskCounts.put(fullname, userTaskCounts.getOrDefault(fullname, 0) + 1);
            } else {
                userTaskCounts.put("Unassigned", userTaskCounts.getOrDefault("Unassigned", 0) + 1);
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_tasks", total);
        stats.put("completed_tasks", completed);
        stats.put("pending_tasks", pending);
        stats.put("high_priority_tasks", highPriority);

        Map<String, Object> stageDistribution = new HashMap<>();
        stageDistribution.put("labels", new ArrayList<>(stageCounts.keySet()));
        stageDistribution.put("data", new ArrayList<>(stageCounts.values()));

        Map<String, Object> userTaskLoad = new HashMap<>();
        userTaskLoad.put("labels", new ArrayList<>(userTaskCounts.keySet()));
        userTaskLoad.put("data", new ArrayList<>(userTaskCounts.values()));

        Map<String, Object> response = new HashMap<>();
        response.put("stats", stats);
        response.put("stage_distribution", stageDistribution);
        response.put("user_task_load", userTaskLoad);

        return response;
    }

    private boolean isValidTransition(String current, String target) {
        // Allow free movement between all valid stages
        List<String> validStages = Arrays.asList("Backlog", "To Do", "In Progress", "Review", "Completed");
        return validStages.contains(current) && validStages.contains(target);
    }
}
