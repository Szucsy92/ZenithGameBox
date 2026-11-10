<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.
<!-- LOVABLE:END -->

- Keep the handheld console presentation in the index route separate from the terminal game modules, so visual framing changes preserve game graphics and mechanics.
- Keep touch-button pointer capture in the shared console controls and adapt pixel drags to game columns in the player, so both hands use the same game input methods.
- Keep launch artwork as imported presentation assets in the index route and derive browser/touch icons from the same app mark, so branding stays consistent without touching game modules.
- Keep Android packaging as an ancillary WebView shell that serves the prerendered client bundle (ZENITH_OFFLINE build) from APK assets for offline play; never replace the TanStack bootstrap or ship server output in the APK.
- Derive Android launch resources from the existing app mark and CSS background token using the branding script, so launch colors remain consistent when the web palette changes.
- Require persistent repository signing secrets for release APKs and use UTC timestamps plus run identifiers for release names, so updates retain the same signer and generations never collide.
