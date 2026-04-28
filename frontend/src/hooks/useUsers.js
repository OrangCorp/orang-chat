import { useState, useEffect } from 'react';
import { userService } from '../services/userService';
import { useAuth } from '../context/AuthContext';

export const useUserProfile = (userId) => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!userId) return;

    const loadProfile = async () => {
      try {
        setLoading(true);
        const data = await userService.getProfile(userId);
        setProfile(data);
        setError(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [userId]);

  return { profile, loading, error };
};

export const useContacts = () => {
  const [contacts, setContacts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { user } = useAuth();

  useEffect(() => {
    if (!user) return;

    const loadContacts = async () => {
      try {
        setLoading(true);
        const data = await userService.getContacts(user.id);
        setContacts(data);
        setError(null);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    loadContacts();
  }, [user]);

  const addContact = async (contactUserId) => {
    try {
      const newContact = await userService.addContact(user.id, contactUserId);
      setContacts(prev => [...prev, newContact]);
      return newContact;
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  const removeContact = async (contactUserId) => {
    try {
      await userService.removeContact(user.id, contactUserId);
      setContacts(prev => prev.filter(c => c.contactUserId !== contactUserId));
    } catch (err) {
      setError(err.message);
      throw err;
    }
  };

  return { contacts, loading, error, addContact, removeContact };
};

export const useSearchUsers = () => {
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const search = async (query) => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    try {
      setLoading(true);
      const data = await userService.searchUsers(query);
      setResults(data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return { results, loading, error, search };
};