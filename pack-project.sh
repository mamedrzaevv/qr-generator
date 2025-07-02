#!/bin/bash

# Универсальный скрипт для упаковки любого проекта
# Использование: ./pack-any-project.sh <путь_к_проекту> [имя_выходного_файла]

if [ $# -eq 0 ]; then
    echo "Использование: $0 <путь_к_проекту> [имя_выходного_файла]"
    echo "Примеры:"
    echo "  $0 .                    # Упаковать текущую папку в project.txt"
    echo "  $0 /path/to/project     # Упаковать проект в project.txt"
    echo "  $0 . my-project.txt     # Упаковать в my-project.txt"
    exit 1
fi

PROJECT_PATH="$1"
OUTPUT_FILE="${2:-project.txt}"

# Проверяем, существует ли JAR-файл в текущей папке
JAR_FILE="target/qr-file-transfer-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR-файл не найден: $JAR_FILE"
    echo "Убедитесь, что $JAR_FILE находится в текущей папке"
    exit 1
fi

# Проверяем, существует ли указанный проект
if [ ! -d "$PROJECT_PATH" ]; then
    echo "Ошибка: Проект не найден: $PROJECT_PATH"
    exit 1
fi

echo "=== Упаковка проекта ==="
echo "Проект: $PROJECT_PATH"
echo "Выходной файл: $OUTPUT_FILE"
echo ""

# Запускаем упаковку
java -cp "$JAR_FILE" com.qrtransfer.ProjectPacker "$PROJECT_PATH" "$OUTPUT_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Проект успешно упакован! ==="
    echo "Файл: $OUTPUT_FILE"
    echo "Размер: $(du -h "$OUTPUT_FILE" | cut -f1)"
    echo ""
    echo "Теперь можно передать этот файл через QR-коды:"
    echo "  java -jar $JAR_FILE encoder"
    echo "  Выберите файл: $OUTPUT_FILE"
else
    echo "ОШИБКА: Упаковка не удалась!"
    exit 1
fi 