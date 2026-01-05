export interface AAModuleLeaf {
  path: string;
  code: string;
  name: string;
  description: string;
  icon?: string;
  status: 'active' | 'development' | 'planned';
}

export interface AAModuleNode {
  name: string;
  icon?: string;
  children?: (AAModuleNode | AAModuleLeaf)[];
}

export type AAModuleTree = AAModuleNode[];

export const isLeaf = (node: AAModuleNode | AAModuleLeaf): node is AAModuleLeaf => {
  return 'path' in node;
};

export const loadAAModulePaths = (): AAModuleTree => [
  {
    name: 'AA',
    icon: 'Layers',
    children: [
      {
        name: 'A0',
        icon: 'FolderOpen',
        children: [
          {
            path: '/modules/AA/A0/AAA0_0100',
            code: 'AAA0_0100',
            name: 'Auth & Worker',
            description: 'Authentication and worker processing',
            icon: 'Shield',
            status: 'active',
          },
          {
            path: '/modules/AA/A0/AAA0_0101',
            code: 'AAA0_0101',
            name: 'OCR Services',
            description: 'Optical Character Recognition',
            icon: 'ScanText',
            status: 'active',
          },
          {
            path: '/modules/AA/A0/AAA0_0102',
            code: 'AAA0_0102',
            name: 'OCR Pipeline',
            description: 'Advanced OCR with detection and recognition',
            icon: 'Workflow',
            status: 'active',
          },
          {
            path: '/modules/AA/A0/AAA0_0103',
            code: 'AAA0_0103',
            name: 'Discharge Paper OCR',
            description: 'YOLO + VietOCR for discharge papers',
            icon: 'FileText',
            status: 'active',
          },
          {
            path: '/modules/AA/A0/AAA0_0104',
            code: 'AAA0_0104',
            name: 'Document Management',
            description: 'Upload and manage OCR documents',
            icon: 'Folder',
            status: 'active',
          },
          {
            path: '/modules/AA/A0/AAA0_0105',
            code: 'AAA0_0105',
            name: 'YOLO Detection',
            description: 'Object detection with YOLO models',
            icon: 'Target',
            status: 'active',
          },
          {
            path: '/modules/AA/A0/AAA0_0201',
            code: 'AAA0_0201',
            name: 'PaddleOCR',
            description: 'PaddlePaddle OCR processing',
            icon: 'FileSearch',
            status: 'development',
          }, {
            path: '/modules/AA/A0/AAA0_0202',
            code: 'AAA0_0202',
            name: 'RAG-VietOCR',
            description: 'RAG-based OCR with VietOCR integration',
            icon: 'FileSearch',
            status: 'development',
          }, {
            path: '/modules/AA/A0/AAA0_0203',
            code: 'AAA0_0203',
            name: 'NER Training Data',
            description: 'Prepare NER training data from AAA0_0202 results',
            icon: 'File',
            status: 'development',
          },
          {
            path: '/modules/AA/A0/AAA0_0300',
            code: 'AAA0_0300',
            name: 'LLM Inference',
            description: 'Large Language Model inference services',
            icon: 'Robo',
            status: 'development',
          }
        ],
      }, {
        name: 'B0',
        icon: 'FolderOpen',
        children: [
          {
            path: '/modules/AA/B0/AAB0_0100',
            code: 'AAB0_0100',
            name: 'Label Studio',
            description: 'Annotation tool for YOLO and OCR',
            icon: 'Tag',
            status: 'active',
          },
        ],
      },
    ],
  },
];

export const getModuleByPath = (path: string): AAModuleLeaf | undefined => {
  const findInTree = (nodes: (AAModuleNode | AAModuleLeaf)[]): AAModuleLeaf | undefined => {
    for (const node of nodes) {
      if (isLeaf(node)) {
        if (node.path === path) return node;
      } else if (node.children) {
        const found = findInTree(node.children);
        if (found) return found;
      }
    }
    return undefined;
  };
  return findInTree(loadAAModulePaths());
};

export const getAllModules = (): AAModuleLeaf[] => {
  const modules: AAModuleLeaf[] = [];
  const collectLeaves = (nodes: (AAModuleNode | AAModuleLeaf)[]) => {
    for (const node of nodes) {
      if (isLeaf(node)) {
        modules.push(node);
      } else if (node.children) {
        collectLeaves(node.children);
      }
    }
  };
  collectLeaves(loadAAModulePaths());
  return modules;
};
