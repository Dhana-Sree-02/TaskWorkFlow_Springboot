package taskflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import taskflow.model.Assignment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByTaskTaskId(Long taskId);
    List<Assignment> findAllByTaskTaskId(Long taskId);
    
    @Modifying
    @Transactional
    void deleteByTaskTaskId(Long taskId);
}
