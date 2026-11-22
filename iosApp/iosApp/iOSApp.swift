import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Must run before launch finishes: a mood chosen in a notification while Anchor was
        // closed is delivered to the handler this installs.
        MainViewControllerKt.setUpMoodReminders()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
