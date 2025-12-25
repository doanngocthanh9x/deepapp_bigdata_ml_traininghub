import os

def print_tree(start_path='.', prefix=''):
    exclude_dirs = {'target', '.git'}
    entries = [e for e in os.listdir(start_path) if e not in exclude_dirs]
    entries.sort()
    for idx, entry in enumerate(entries):
        path = os.path.join(start_path, entry)
        connector = '└── ' if idx == len(entries) - 1 else '├── '
        print(prefix + connector + entry)
        if os.path.isdir(path):
            extension = '    ' if idx == len(entries) - 1 else '│   '
            print_tree(path, prefix + extension)


if __name__ == "__main__":
    print_tree(os.getcwd())