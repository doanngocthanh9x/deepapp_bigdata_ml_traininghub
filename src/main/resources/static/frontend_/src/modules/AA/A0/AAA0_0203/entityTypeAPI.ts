// Entity Type types and interfaces
export interface EntityType {
  id: number;
  template_id: string | null;
  entity_code: string;
  display_label: string;
  description?: string;
  color?: string;
  icon?: string;
  display_order: number;
  active: boolean;
  examples?: string;
  created_at: string;
  updated_at: string;
}

export interface EntityTypeFormData {
  template_id?: string;
  entity_code: string;
  display_label: string;
  description?: string;
  color?: string;
  icon?: string;
  display_order?: number;
  examples?: string;
}

// Helper function to convert EntityType to legacy format for backward compatibility
export function convertToLegacyFormat(entityType: EntityType) {
  return {
    label: entityType.display_label,
    color: `bg-${entityType.color}-100 text-${entityType.color}-800 border-${entityType.color}-200`,
    icon: entityType.icon
  };
}

// API functions
export const EntityTypeAPI = {
  async getEntityTypes(templateId?: string): Promise<EntityType[]> {
    const url = templateId 
      ? `/AA/A0/AAA0_0203/entity-types?template_id=${templateId}`
      : '/AA/A0/AAA0_0203/entity-types/global';
    
    const response = await fetch(url);
    const data = await response.json();
    
    if (data.status === 'success') {
      return data.entity_types;
    }
    throw new Error(data.message || 'Failed to load entity types');
  },

  async createEntityType(formData: EntityTypeFormData): Promise<EntityType> {
    const response = await fetch('/AA/A0/AAA0_0203/entity-types', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    });
    
    const data = await response.json();
    
    if (data.status === 'success') {
      return data.data.entity_type;
    }
    throw new Error(data.message || 'Failed to create entity type');
  },

  async updateEntityType(id: number, formData: Partial<EntityTypeFormData>): Promise<EntityType> {
    const response = await fetch(`/AA/A0/AAA0_0203/entity-types/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    });
    
    const data = await response.json();
    
    if (data.status === 'success') {
      return data.data.entity_type;
    }
    throw new Error(data.message || 'Failed to update entity type');
  },

  async deleteEntityType(id: number): Promise<void> {
    const response = await fetch(`/AA/A0/AAA0_0203/entity-types/${id}`, {
      method: 'DELETE'
    });
    
    const data = await response.json();
    
    if (data.status !== 'success') {
      throw new Error(data.message || 'Failed to delete entity type');
    }
  },

  async initializeDefaultEntityTypes(): Promise<void> {
    const response = await fetch('/AA/A0/AAA0_0203/entity-types/initialize', {
      method: 'POST'
    });
    
    const data = await response.json();
    
    if (data.status !== 'success') {
      throw new Error(data.message || 'Failed to initialize default entity types');
    }
  }
};
