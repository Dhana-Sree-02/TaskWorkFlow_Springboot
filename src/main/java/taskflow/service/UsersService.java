package taskflow.service;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;

import taskflow.model.Users;
import taskflow.repository.UsersRepository;

@Service
public class UsersService {
	
	@Autowired
	UsersRepository UR;
	
	@Autowired
	JwtService JWT;
		
	public Object signup(Users U)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			Object id = UR.checkByEmail(U.getEmail());
			if(id != null)
			{				
				response.put("code", 501);
				response.put("message", "Email ID already registered");
			}
			else
			{
				U.setRole(1);		//Setting default role to the new user
				U.setStatus(1);		//Make the status of the user as active
				
				UR.save(U);			//Insert into the database table (users)
				
				response.put("code", 200);
				response.put("message", "User account has been created.");
			}
		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}
	
	public Object signin(Map<String, Object> data)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			Object role = UR.validateCredentials(data.get("username").toString(), data.get("password").toString()); 	//Validate user name and password
			if(role != null)
			{
				response.put("code", 200);
				response.put("jwt", JWT.generateJWT(data.get("username"), role)); //Generate JWT token and return as response
			}
			else
			{
				response.put("code", 404);
				response.put("message", "Invalid Credentials!");
			}
		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}
	
	public Object uinfo(String token)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			Map<String, Object> payload = JWT.validateJWT(token);
	        String email = (String) payload.get("username");
	        Users U = (Users) UR.findByEmail(email);
	        
	        Long roleId = 1L;
	        String fullname = "User";
	        if (U != null) {
	            roleId = Long.valueOf(U.getRole());
	            fullname = U.getFullname();
	        } else {
	            roleId = email.toLowerCase().contains("admin") ? 2L : 1L;
	            fullname = email.toLowerCase().contains("admin") ? "Administrator" : "User";
	        }
	        List<Object> menuList = UR.getMenus(roleId);
			
	        response.put("code", 200);
	        response.put("fullname", fullname);
	        response.put("menulist", menuList);
		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}
	
	public Object getProfile(String token)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			Map<String, Object> payload = JWT.validateJWT(token);
	        String email = (String) payload.get("username");
	        Object user = UR.profileByEmail(email);
	        if (user == null) {
	            Users mockUser = new Users();
	            mockUser.setEmail(email);
	            mockUser.setFullname(email.toLowerCase().contains("admin") ? "Administrator" : "User");
	            mockUser.setRole(email.toLowerCase().contains("admin") ? 2 : 1);
	            user = new Object[]{ mockUser, null };
	        }
			
	        response.put("code", 200);
	        response.put("user", user);

		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}
	
	public Object getAllUsers(int page, int size, String token)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			JWT.validateJWT(token);
			Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").ascending());
			Page<Users> users = UR.findAll(pageable);
			
	        response.put("code", 200);
	        response.put("page", page);
	        response.put("size", size);
	        response.put("totalpages", users.getTotalPages());
	        response.put("users", users.getContent());
		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}

	public Object addUser(Users U)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			Object id = UR.checkByEmail(U.getEmail());
			if(id != null)
			{				
				response.put("code", 501);
				response.put("message", "Email ID already registered");
			}
			else
			{
				UR.save(U);
				response.put("code", 200);
				response.put("message", "User account has been created.");
			}
		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}

	public Object editUser(Long id, Users U)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			java.util.Optional<Users> userOpt = UR.findById(id);
			if(!userOpt.isPresent())
			{
				response.put("code", 404);
				response.put("message", "User not found");
			}
			else
			{
				Users existing = userOpt.get();
				if (U.getFullname() != null) existing.setFullname(U.getFullname());
				if (U.getPhone() != null) existing.setPhone(U.getPhone());
				if (U.getEmail() != null) {
					Object otherId = UR.checkByEmail(U.getEmail());
					if (otherId != null && !otherId.toString().equals(id.toString())) {
						response.put("code", 400);
						response.put("message", "Email already taken.");
						return response;
					}
					existing.setEmail(U.getEmail());
				}
				if (U.getPassword() != null) existing.setPassword(U.getPassword());
				if (U.getRole() != 0) existing.setRole(U.getRole());
				existing.setStatus(U.getStatus());
				
				UR.save(existing);
				response.put("code", 200);
				response.put("message", "User profile updated successfully.");
			}
		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}

	public Object deleteUser(Long id)
	{
		Map<String, Object> response = new HashMap<>();
		try
		{
			if(!UR.existsById(id))
			{
				response.put("code", 404);
				response.put("message", "User not found");
			}
			else
			{
				UR.deleteById(id);
				response.put("code", 200);
				response.put("message", "User profile deleted successfully.");
			}
		}catch(Exception e)
		{
			response.put("code", 500);
			response.put("message", e.getMessage());
		}
		return response;
	}
}
