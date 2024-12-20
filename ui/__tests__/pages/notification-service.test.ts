import { NotificationService } from "../../app/manager/src/pages/page-notifications";
import { SentNotification } from '@openremote/model';
import manager from "@openremote/core";

// Mock manager instead of trying to access it through the service
jest.mock('@openremote/core', () => ({
    __esModule: true, // needed for ES modules
    default: {
        rest: {
            api: {
                NotificationResource: {
                    getAllNotifications: jest.fn()
                }
            }
        }
    }
}));

// minimal test
describe('NotificationService', () => {
    it('should import core', () => {
        expect(manager).toBeDefined();
    });
});

describe('NotificationService', () => {
    let service: NotificationService;
    
    beforeEach(() => {
        service = new NotificationService();
        // Clear all mocks between tests
        jest.clearAllMocks();
    });

    it('should fetch notifications successfully', async () => {
        // Setup
        const mockNotifications = [{
            id: 1,
            name: 'Test Notification',
            type: 'push'
        }];

        // Mock the manager's API call directly
        (manager.rest.api.NotificationResource.getAllNotifications as jest.Mock)
            .mockResolvedValue({
                data: mockNotifications,
                status: 200
            });

        // Test
        const result = await service.getNotifications('test-realm');

        // Verify
        expect(result).toEqual(mockNotifications);
        // Verify the API was called
        expect(manager.rest.api.NotificationResource.getAllNotifications).toHaveBeenCalled();
    });

    it('should handle API errors gracefully', async () => {
        // Mock API failure
        (manager.rest.api.NotificationResource.getAllNotifications as jest.Mock)
            .mockRejectedValue(new Error('API Error'));

        // Test & verify
        await expect(service.getNotifications('test-realm'))
            .rejects
            .toThrow('API Error');
    });
});