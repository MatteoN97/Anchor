import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Compose draws edge to edge and reads the safe area itself (WindowInsets.safeDrawing).
        // Keeping SwiftUI's safe area left white bands above and below the app.
        ComposeView()
            .ignoresSafeArea()
    }
}
