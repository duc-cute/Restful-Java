package vn.hoidanit.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.hoidanit.jobhunter.domain.Permission;

import java.util.List;

@Repository
public interface PermissionRepository  extends JpaRepository<Permission,Long>, JpaSpecificationExecutor<Permission> {
    boolean existsByModuleAndApiPathAndMethod(String module,String apiPath,String method);

    boolean existsByName(String name);
    List<Permission> findByIdIn(List<Long> id);

}
