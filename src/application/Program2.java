package application;

import java.util.ArrayList;
import java.util.List;

import model.dao.DaoFactory;
import model.dao.DepartmentDao;
import model.entities.Department;

public class Program2 {

	public static void main(String[] args) {
		DepartmentDao departmentDao = DaoFactory.createDepartmentDao();
		
		System.out.println("=== TEST1: department findById ===");
		Department department = departmentDao.findById(3);
		System.out.println(department);
		
		List<Department> list = new ArrayList<>();
		
		System.out.println("\n=== TEST2: department findAll ===");
		list = departmentDao.findAll();
		for(Department obj : list) {
			System.out.println(obj);
		}
		
		System.out.println("\n=== TEST3: department insert ===");
		Department newdepartment = new Department(null, "RH");
		departmentDao.insert(newdepartment);
		System.out.println("Inserted! New id = " + newdepartment.getId());
		
		System.out.println("\n=== TEST4: department update ===");
		department = departmentDao.findById(4);
		department.setName("Food");
		departmentDao.update(department);
		System.out.println("Update completed");
		
		System.out.println("\n=== TEST5: department delete ===");
		departmentDao.deleteById(8);
		departmentDao.deleteById(9);
		System.out.println("Delete completed");
	}
}
