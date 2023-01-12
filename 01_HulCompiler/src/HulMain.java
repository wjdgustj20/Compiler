/*
 * OS: Windows 11
 * Compiler: javac 1.8.0_261
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class HulMain {
	public static void main(String[] args) {
		String hul_program = ""; // Hul 소스 파일 내용을 읽어와 저장할 문자열
		
		File file = new File("./test.hul"); // input 파일
		
		try (FileInputStream fstream = new FileInputStream(file);) { // input 파일 스트림 열기
			byte[] rb = new byte[fstream.available()];
			while (fstream.read(rb) != -1) {}
			fstream.close();
			hul_program = new String(rb); // input 파일 스트림을 문자열 hul_program에 저장
		} catch (Exception e) {
			e.getStackTrace();
		}
		
		try (FileWriter fw = new FileWriter("./test.c");) { // output 파일을 작성하기 위한 FileWriter
			HulCompiler hc = new HulCompiler(fw, hul_program); // Hul을 C코드로 바꿔주는 프로그램 클래스 객체 생성, 인자로 FileWriter와 파일 내용 전달
			hc.compile(); // Hul to C
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
