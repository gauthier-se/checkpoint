package com.seyzeriat.desktop.di;

import com.seyzeriat.desktop.controller.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DependencyContainerTest {

    @Test
    void testGetInstanceReturnsSingleton() {
        DependencyContainer instance1 = DependencyContainer.getInstance();
        DependencyContainer instance2 = DependencyContainer.getInstance();

        assertNotNull(instance1, "Instance should not be null");
        assertSame(instance1, instance2, "Instances should be the same (Singleton)");
    }

    @Test
    void testCreateControllerResolvesDependencies() {
        DependencyContainer container = DependencyContainer.getInstance();

        Object loginController = container.createController(LoginController.class);
        assertTrue(loginController instanceof LoginController, "Should return a LoginController");

        Object manageGamesController = container.createController(ManageGamesController.class);
        assertTrue(manageGamesController instanceof ManageGamesController, "Should return a ManageGamesController");

        Object userDetailController = container.createController(UserDetailController.class);
        assertTrue(userDetailController instanceof UserDetailController, "Should return a UserDetailController");

        Object reviewModerationController = container.createController(ReviewModerationController.class);
        assertTrue(reviewModerationController instanceof ReviewModerationController, "Should return a ReviewModerationController");

        Object reportModerationController = container.createController(ReportModerationController.class);
        assertTrue(reportModerationController instanceof ReportModerationController, "Should return a ReportModerationController");
    }

    @Test
    void testCreateControllerThrowsExceptionForUnknownClassWithNoDefaultConstructor() {
        DependencyContainer container = DependencyContainer.getInstance();

        class UnknownController {
            public UnknownController(String fakeDep) {}
        }

        assertThrows(RuntimeException.class, () -> container.createController(UnknownController.class),
                "Should throw RuntimeException when unable to resolve dependencies for an unknown controller");
    }
}
