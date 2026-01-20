import os
import sys

# Configuración de rutas relativas
# Asume que este script está en ../DictionaryTools y el proyecto en ../LMMfunction
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_RES_DIR = os.path.join(BASE_DIR, '..', 'LMMfunction', 'src', 'main', 'resources', 'dictionaries')

BLACKLIST_FILE = os.path.join(PROJECT_RES_DIR, 'blacklist.txt')
BOOSTLIST_FILE = os.path.join(PROJECT_RES_DIR, 'boostlist.txt')
THESAURUS_FILE = os.path.join(PROJECT_RES_DIR, 'thesaurus.txt')

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def ensure_dir():
    if not os.path.exists(PROJECT_RES_DIR):
        print(f"❌ Error: No encuentro la carpeta de recursos en: {PROJECT_RES_DIR}")
        print("Asegúrate de que la estructura de carpetas es correcta.")
        sys.exit(1)

def add_word(filename, list_name):
    print(f"\n--- AÑADIR A {list_name.upper()} ---")
    print("Escribe las palabras (una por línea). Escribe 'salir' para terminar.")
    
    new_words = []
    while True:
        word = input("> ").strip().lower()
        if word == 'salir':
            break
        if word:
            new_words.append(word)
    
    if new_words:
        with open(filename, 'a', encoding='utf-8') as f:
            for w in new_words:
                f.write(f"{w}\n")
        print(f"✅ Se añadieron {len(new_words)} palabras.")
    else:
        print("No se añadieron palabras.")
    input("Presiona Enter para continuar...")

def view_list(filename, list_name):
    print(f"\n--- CONTENIDO DE {list_name.upper()} ---")
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            for i, line in enumerate(lines):
                print(f"{i+1}. {line.strip()}")
            print(f"\nTotal: {len(lines)} palabras.")
    except FileNotFoundError:
        print("El archivo aún no existe.")
    input("Presiona Enter para continuar...")

def add_thesaurus_entry():
    print(f"\n--- AÑADIR RELACIÓN (SINÓNIMOS/CONTEXTO) ---")
    print("Esto ayuda al sistema a entender qué palabras están relacionadas.")
    print("Ejemplo -> Clave: 'coches' | Relacionados: 'ferrari, bmw, audi, velocidad'")
    
    key = input("\nPalabra Clave (Lo que busca el usuario): ").strip().lower()
    if not key: return
    
    related = input(f"Palabras relacionadas con '{key}' (separadas por coma): ").strip().lower()
    if not related: return
    
    # Formato simple: clave:valor1,valor2,valor3
    entry = f"{key}:{related}\n"
    
    with open(THESAURUS_FILE, 'a', encoding='utf-8') as f:
        f.write(entry)
    print(f"✅ Relación guardada: {key} -> [{related}]")
    input("Presiona Enter para continuar...")

def view_thesaurus():
    print(f"\n--- DICCIONARIO DE RELACIONES ---")
    try:
        with open(THESAURUS_FILE, 'r', encoding='utf-8') as f:
            for line in f:
                if ':' in line:
                    key, values = line.strip().split(':', 1)
                    print(f"🔑 {key.upper()}: {values}")
    except FileNotFoundError:
        print("El archivo aún no existe.")
    input("Presiona Enter para continuar...")

def main_menu():
    ensure_dir()
    while True:
        clear_screen()
        print("========================================")
        print("   GESTOR DE DICCIONARIOS PERSEPOLIS    ")
        print("========================================")
        print(f"Ruta: {PROJECT_RES_DIR}")
        print("----------------------------------------")
        print("1. 🚫 Añadir a BLACKLIST (Excluir)")
        print("2. 🚀 Añadir a BOOSTLIST (Relevancia)")
        print("3. 👁️  Ver Blacklist")
        print("4. 👁️  Ver Boostlist")
        print("5. 🔗 Añadir Relación (Thesaurus)")
        print("6. 👁️  Ver Relaciones")
        print("0. Salir")
        
        op = input("\nElige una opción: ")
        
        if op == '1':
            add_word(BLACKLIST_FILE, "Blacklist")
        elif op == '2':
            add_word(BOOSTLIST_FILE, "Boostlist")
        elif op == '3':
            view_list(BLACKLIST_FILE, "Blacklist")
        elif op == '4':
            view_list(BOOSTLIST_FILE, "Boostlist")
        elif op == '5':
            add_thesaurus_entry()
        elif op == '6':
            view_thesaurus()
        elif op == '0':
            break
        else:
            print("Opción no válida.")

if __name__ == "__main__":
    main_menu()