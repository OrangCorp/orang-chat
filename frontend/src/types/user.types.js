export const ContactStatus = {
  PENDING: 'PENDING',
  ACCEPTED: 'ACCEPTED',
  BLOCKED: 'BLOCKED'
};

/**
 * ProfileResponse format:
 * {
 *   userId: "uuid",
 *   displayName: "John Doe",
 *   avatarUrl: "https://...",
 *   bio: "About me...",
 *   lastSeen: "2026-03-18T12:00:00",
 *   isOnline: true
 * }
 */

/**
 * ContactResponse format:
 * {
 *   id: "uuid",
 *   userId: "uuid",
 *   contactUserId: "uuid",
 *   displayName: "Jane Doe",
 *   avatarUrl: "https://...",
 *   status: "PENDING" | "ACCEPTED" | "BLOCKED",
 *   isOnline: true,
 *   createdAt: "2026-03-18T12:00:00"
 * }
 */

/**
 * UpdateProfileRequest format:
 * {
 *   displayName: "New Name",     // optional
 *   avatarUrl: "new-avatar.jpg", // optional
 *   bio: "New bio"               // optional
 * }
 */