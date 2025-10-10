package application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import model.entities.Department;
import model.entities.Seller;

public class Program {

	public static void main(String[] args) {
		
		DateTimeFormatter fmt1 = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
		Department obj = new Department(1, "Books");
		System.out.println(obj);
		
		LocalDate d08 = LocalDate.parse("20/07/1992", fmt1);
		
		Seller seller = new Seller(21, "Bob", "bob@gmail.com", d08, 3000.0, obj);
		
		System.out.println(seller);
	}

}
