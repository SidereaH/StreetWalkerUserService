package streetwalker.userservice.services;

import java.util.concurrent.ThreadLocalRandom;

public class CodeGenerator {

    /**
     * Генерирует 4-значный числовой код
     * @return 4-значный код в диапазоне от 1000 до 9999 (включительно)
     */
    public static long generate4DigitCode() {
        // Генерируем случайное число от 1000 до 9999
        return ThreadLocalRandom.current().nextLong(1000, 10000);
    }

    // Пример использования
//    public static void main(String[] args) {
//        long code = generate4DigitCode();
//        System.out.println("Сгенерированный код: " + code);
//    }
}