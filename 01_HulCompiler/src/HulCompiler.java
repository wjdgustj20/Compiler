import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HulCompiler { // Hul을 C코드로 바꿔주는 프로그램 클래스
	FileWriter fw; // output 파일 작성을 위한 FileWriter
	String hul_program; // Hul 소스 파일의 내용을 저장할 문자열
	
	// 전후 기본 코드
	private static final String prologue = "#include <stdio.h>\n" + 
											"int main() {\n" +
											"\tint _hul;\n\n";
	
	private static final String epilogue = "\n\treturn 0;\n" +
											"}";
	
	// loop의 중첩 개수를 count할 변수(loop 내 변수이름을 정의하기 위함. 실제 중첩 개수는 loop_depth+1 임.)
	static int loop_depth = 0;
	
	// 생성자, FileWriter 객체와 Hul 소스 파일 내용을 파라미터로 함.
	public HulCompiler(FileWriter fw, String hul_program) throws IOException {
		this.fw = fw;
		this.hul_program = hul_program;
		fw.write(prologue);
	}
	
	// 반복 외 연산을 처리하는 함수
	public String[] operate(String word) {
		String[] code = new String[2]; // Hul? 의 경우 표준 입력과 변수 저장을 줄 바꿈하기 위해 크기 2인 배열을 사용함.
		code[0] = "";
		code[1] = "\n";
		
		if (word.charAt(3) == '?') { // Hul?
			code[0] = "\tprintf(\"input: \");\n";
			code[1] = "\tscanf(\"%d\", &_hul);\n";
		}
		else if (word.charAt(3) == '!') { // Hul!
			code[0] = "\tprintf(\"%d\", _hul);";
		}
		else if (word.charAt(3) == '>') { // Hul>
			code[0] = "\t_hul++;";
		}
		else if (word.charAt(3) == '<') { // Hul<
			code[0] = "\t_hul--;";
		}
		
		return code;
	}
	
	// 반복 연산을 처리하는 함수, 반복문 내부 명령들과 반복 횟수를 파라미터로 한다.
	public String operate_repetition(List<String> innerwords, int count) {
		String code = ""; // C코드를 저장할 문자열 변수
		
		// 반복문 중첩 시 괄호의 짝을 맞추기 위해 여는 괄호와 닫는 괄호 개수를 세는 변수
		int number_of_left_paren = 0;
		int number_of_right_paren = 0;
		
		// 반목문 기본 코드, loop_depth를 이용해 변수 선언
		code = "\tint max" + loop_depth + " = " + count + ";\n" +
				"\tfor (int i" + loop_depth + " = 0; i" + loop_depth + " < max" + loop_depth + "; i" + loop_depth + "++) {\n";
		
		// 반복문 내부 명령들을 처리
		for (int i = 0; i < innerwords.size(); i++) {
			if (innerwords.get(i).charAt(3) == '{') { // 내부 명령이 반복문인 경우(중첩 반복)
				loop_depth++; // 중첩 개수 변수를 증가
				number_of_left_paren++; // 여는 괄호의 개수 증가
				List<String> innerinnerwords = new ArrayList<String>(); // 중첩된 내부 반복문의 명령들을 저장할 리스트
				while(number_of_left_paren != number_of_right_paren) { // 짝이 맞는 닫는 괄호가 나올 때까지 반복
					innerinnerwords.add(innerwords.get(++i)); // 내부 반복문의 명령들을 innerinnerwords에 저장
					if ((innerwords.get(i).length() >= 4) && (innerwords.get(i).charAt(3) == '{')) { // 내부 반복문 속 또 중첩된 반복문이 있는 경우(여는 괄호가 나오는 경우)
						number_of_left_paren++; // 여는 괄호 개수 증가
					}
					else if (innerwords.get(i).charAt(0) == '}') { // 닫는 괄호가 나오는 경우
						number_of_right_paren++; // 닫는 괄호 개수 증가
					}
				}
				// 여는 괄호와 닫는 괄호의 개수가 일치하는 경우 while문을 탈출하고
				
				int inner_count = Integer.parseInt(innerwords.get(++i)); // 반복 횟수를 저장
				innerinnerwords.remove(innerinnerwords.size() - 1); // innerinnerwords에 저장된 바깥 반복문의 닫는 괄호 제거
				Scanner sc = new Scanner(operate_repetition(innerinnerwords, inner_count)); // 내부 반복문을 재귀를 통해 현재 함수를 다시 불러와 처리
				
				// 내부 반복문 코드를 적절한 들여쓰기 추가 후 code 변수에 저장
				while(sc.hasNextLine()) {
					String temp = "\t" + sc.nextLine() + "\n";
					code += temp;
				}
				sc.close();
			}
			else { // 내부 명령이 반복문이 아닌 경우
				String[] temp = operate(innerwords.get(i)); // 반복 외 연산을 처리하는 함수 호출
				
				// 반환된 C코드를 code 변수에 적절히 저장
				code += "\t" + temp[0] +
						"\t" + temp[1];
			}
		}
		code += "\t}\n";
		
		return code;
	}
	
	
	public void compile() throws IOException { // Hul을 C코드로 바꿔주는 함수
		String[] words = hul_program.split("[\n\t \r]+"); // Hul 프로그램 소스 파일을 공백으로 구분해 문자열 배열에 저장
		String code = ""; // 변환한 C코드를 저장할 변수
		
		// 반복문 중첩 시 괄호의 짝을 맞추기 위해 여는 괄호와 닫는 괄호 개수를 세는 변수
		int number_of_left_paren = 0;
		int number_of_right_paren = 0;
		
		// words 배열에 있는 명령들을 차례로 처리
		for (int i = 0; i < words.length; i++) {
			if (words[i].charAt(3) == '{') { // 반복문의 경우
				number_of_left_paren++; // 여는 괄호 개수 증가
				List<String> innerwords = new ArrayList<String>(); // 반복문 내부의 명령들을 저장할 리스트
				while(number_of_left_paren != number_of_right_paren) { // 짝이 맞는 닫는 괄호가 나올 때까지 반복
					innerwords.add(words[++i]); // 반복문 내부의 명령들을 innerwords에 저장
					if ((words[i].length() >= 4) && (words[i].charAt(3) == '{')) { // 반복문 속 중첩된 반복문이 있는 경우(여는 괄호가 나오는 경우)
						number_of_left_paren++; // 여는 괄호 개수 증가
					}
					else if (words[i].charAt(0) == '}') { // 닫는 괄호가 나오는 경우
						number_of_right_paren++; // 닫는 괄호 개수 증가
					}
				}
				// 여는 괄호와 닫는 괄호의 개수가 일치하는 경우 while문을 탈출하고
				
				int count = Integer.parseInt(words[++i]); // 반복 횟수를 저장
				innerwords.remove(innerwords.size() - 1); // innerwords에 저장된 바깥 반복문의 닫는 괄호 제거
				code = operate_repetition(innerwords, count); // 반복문을 처리하는 함수 호출
				loop_depth = 0; // 가장 바깥 반복문이 모두 처리된 경우 loop_depth는 다시 0이 됨.
			}
			else { // 내부 명령이 반복문이 아닌 경우
				code = operate(words[i])[0] + operate(words[i])[1]; // 반복 외 연산을 처리하는 함수 호출하고 반환된 C코드를 code 변수에 적절히 저장
			}
			
			try { // FileWriter를 이용해 code를 output 파일에 출력
				fw.write(code);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			code = ""; // 다음 명령 처리를 위해 code 문자열 비우기
		}
		
		fw.write(epilogue);
	}
}
