// services/attachmentService.js
const API_BASE_URL = '/api';

const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Authorization': `Bearer ${token}`
  };
};

class AttachmentService {
  async upload(file, conversationId) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('conversationId', conversationId);

    const response = await fetch(`${API_BASE_URL}/attachments/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
      },
      body: formData
    });

    if (!response.ok) {
      const err = await response.text();
      throw new Error(`Upload failed: ${response.status} ${err}`);
    }
    return response.json();
  }

  async getAttachment(attachmentId) {
    const response = await fetch(`${API_BASE_URL}/attachments/${attachmentId}`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to get attachment info');
    return response.json();
  }

  // Download file with auth and trigger browser download
  async downloadFile(attachmentId, fileName) {
    const response = await fetch(`${API_BASE_URL}/attachments/${attachmentId}/download`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Download failed');
    
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName || 'download';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // Get thumbnail as a blob URL for <img> tags
  async getThumbnailBlobUrl(attachmentId) {
    const response = await fetch(`${API_BASE_URL}/attachments/${attachmentId}/thumbnail`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Thumbnail failed');
    const blob = await response.blob();
    return URL.createObjectURL(blob);
  }

  async deleteAttachment(attachmentId) {
    const response = await fetch(`${API_BASE_URL}/attachments/${attachmentId}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to delete attachment');
  }
}

export default new AttachmentService();