import { useAuthStore } from '../authStore';

describe('authStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    const { clearError } = useAuthStore.getState();
  });

  it('should initialize with no user', () => {
    const { user } = useAuthStore.getState();
    expect(user).toBeNull();
  });

  it('should have clearError method', () => {
    const { clearError } = useAuthStore.getState();
    expect(typeof clearError).toBe('function');
  });

  it('should have isInitializing flag', () => {
    const { isInitializing } = useAuthStore.getState();
    expect(typeof isInitializing).toBe('boolean');
  });

  it('should have isLoading flag', () => {
    const { isLoading } = useAuthStore.getState();
    expect(typeof isLoading).toBe('boolean');
  });
});
