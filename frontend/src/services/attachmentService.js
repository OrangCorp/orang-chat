// services/attachmentService.js
const API_BASE_URL = '/api';

const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Authorization': `Bearer ${token}`
    // Note: don't set Content-Type for multipart
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
        // Let browser set Content-Type with boundary for FormData
      },
      body: formData
    });

    if (!response.ok) {
      const err = await response.text();
      throw new Error(`Upload failed: ${response.status} ${err}`);
    }
    return response.json(); // AttachmentResponse
  }

  async getAttachment(attachmentId) {
    const response = await fetch(`${API_BASE_URL}/attachments/${attachmentId}`, {
      headers: getHeaders()
    });
    if (!response.ok) throw new Error('Failed to get attachment info');
    return response.json();
  }

  async getDownloadUrl(attachmentId) {
    // The download endpoint directly returns the file or presigned URL.
    // We can just construct the URL; the browser will download.
    return `${API_BASE_URL}/attachments/${attachmentId}/download`;
  }

  async getThumbnailUrl(attachmentId) {
    return `${API_BASE_URL}/attachments/${attachmentId}/thumbnail`;
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