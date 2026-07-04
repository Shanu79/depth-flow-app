package com.shin.depthflow

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.shin.depthflow.feature.billing.BillingScreen
import com.shin.depthflow.feature.contact.ContactUsScreen
import com.shin.depthflow.feature.faq.FAQScreen
import com.shin.depthflow.feature.projects.MyProjectsScreen
import com.shin.depthflow.feature.home.DepthFlowHomeScreen
import com.shin.depthflow.feature.pricing.PricingScreen
import com.shin.depthflow.feature.projects.MyProjectsScreen
import com.shin.depthflow.feature.profile.EditProfileScreen
import com.shin.depthflow.feature.profile.ProfileScreen
import com.shin.depthflow.feature.workspace.WorkspaceScreen
import com.shin.depthflow.ui.AuthScreen
import com.shin.depthflow.ui.OtpVerificationScreen
import com.shin.depthflow.ui.ResetPasswordScreen
import com.shin.depthflow.ui.PlusButtonModalSheet
import com.shin.depthflow.ui.navigation.DepthFlowBottomNavigation
import com.shin.depthflow.ui.navigation.DepthFlowTopBar
import com.shin.depthflow.ui.navigation.Routes
import com.shin.depthflow.ui.theme.Surface
import com.shin.depthflow.viewmodel.DepthFlowViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

// Define your routes object
@Composable
fun DepthFlowApp(
    viewModel: DepthFlowViewModel,
    // Callback to pass billing intents from the Pricing UI back to MainActivity
    onInitiatePurchase: (planId: String, isYearly: Boolean) -> Unit = {_, _ -> },
    // Inject the initial start destination from MainActivity
    startDestination: String = Routes.HOME
) {
    val navController = rememberNavController()

    // Track the current route to highlight the correct bottom nav icon & hide bars
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    // Observe the global credits from the ViewModel to feed into the TopBar
    val currentCredits by viewModel.credits.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // State for the central Plus Button Modal
    var showPlusOverlay by remember { mutableStateOf(false) }

    // We do not want the TopBar or BottomBar to appear on the Login Screen
    val showNavigationBars = currentRoute != Routes.LOGIN

    val routesWithoutTopBar = listOf(
        Routes.LOGIN,
        Routes.PRICING,
        Routes.CONTACT_US,
        Routes.BILLING,
        Routes.FAQ,
        Routes.MY_PROJECTS,
        Routes.EDIT_PROFILE,
        Routes.REGISTER,
        Routes.VERIFY_OTP,
        Routes.RESET_PASSWORD
    )

    val hazeState = remember { HazeState() }

    val showTopBar = currentRoute !in routesWithoutTopBar


    Scaffold(
        containerColor = Color.Transparent, // MUST be transparent for Box background to be sampled
        topBar = {
            if (showTopBar) {
                DepthFlowTopBar(
                    credits = currentCredits,
                    isLoggedIn = isLoggedIn,
                    onLoginClick = {
                        navController.navigate(Routes.LOGIN)
                    }
                )
            }
        },
        bottomBar = {
            if (showNavigationBars) {
                // 2. The floating styling and system bar protection are applied directly here
                DepthFlowBottomNavigation(
                    modifier = Modifier
                        .fillMaxWidth(),
                    hazeState = hazeState,
                    isModalOpen = showPlusOverlay,
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        val isProtectedRoute = route == Routes.WORKSPACE || route == Routes.PROFILE
                        if (isProtectedRoute && !isLoggedIn) {
                            navController.navigate(Routes.LOGIN)
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onPlusClicked = {
                        showPlusOverlay = !showPlusOverlay
                    }
                )
            }
        }
    ) { innerPadding ->

        // 3. The NavHost receives the innerPadding, ensuring your content
        // never gets permanently hidden behind the floating bar.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(Surface)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
            ) {
                // --- LOGIN ROUTE ---
                composable(Routes.LOGIN) {
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    val credentialManager = remember { CredentialManager.create(context) }

                    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

                    AuthScreen(
                        viewModel = viewModel,
                        initialIsLogin = true,
                        onLoginSuccess = { token ->
                            // 1. Give the token to the ViewModel to fetch credits
                            viewModel.setAuthToken(token)

                            // 2. Navigate to Home and destroy the Login screen from the backstack
                            navController.navigate(Routes.WORKSPACE) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onRegisterSuccess = {
                            navController.navigate(Routes.VERIFY_OTP)
                        },
                        onForgotPasswordRequestSuccess = {
                            navController.navigate(Routes.RESET_PASSWORD)
                        },
                        onGoogleSignInClick = {
                            if (webClientId.isBlank()) {
                                Toast.makeText(context, "Config Error: Google Client ID is missing", Toast.LENGTH_LONG).show()
                                return@AuthScreen
                            }
                            coroutineScope.launch {
                                try {
                                    // Build modern Google ID request parameters
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .setNonce(generateSecureNonce())
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    // Fires the native Android account selection system sheet
                                    val result = credentialManager.getCredential(
                                        context = context,
                                        request = request
                                    )

                                    val credential = result.credential
                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken

                                        // Pass the ID token directly to your ViewModel to trade for a session token
                                        viewModel.loginWithGoogle(idToken)
                                    }
                                } catch (e: GetCredentialException) {
                                    Log.e("DepthFlowApp", "Google Sign-In error: ${e.type}", e)
                                    Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("DepthFlowApp", "Unexpected error during Google Sign-In", e)
                                    Toast.makeText(context, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onClose = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // --- REGISTER ROUTE ---
                composable(Routes.REGISTER) {
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    val credentialManager = remember { CredentialManager.create(context) }

                    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID

                    AuthScreen(
                        viewModel = viewModel,
                        initialIsLogin = false,
                        onLoginSuccess = { token ->
                            viewModel.setAuthToken(token)
                            navController.navigate(Routes.WORKSPACE) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onRegisterSuccess = {
                            navController.navigate(Routes.VERIFY_OTP)
                        },
                        onForgotPasswordRequestSuccess = {
                            navController.navigate(Routes.RESET_PASSWORD)
                        },
                        onGoogleSignInClick = {
                            // Similar Google Sign-In logic if needed on Register screen
                        },
                        onClose = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.VERIFY_OTP) {
                    OtpVerificationScreen(
                        viewModel = viewModel,
                        onVerificationSuccess = { token ->
                            viewModel.setAuthToken(token)
                            navController.navigate(Routes.WORKSPACE) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onBackToRegister = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.RESET_PASSWORD) {
                    ResetPasswordScreen(
                        viewModel = viewModel,
                        onResetSuccess = {
                            navController.navigate(Routes.LOGIN) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Routes.HOME) {
                    DepthFlowHomeScreen(
                        onNavigateToWorkspace = {
                            if(isLoggedIn) {
                                navController.navigate(Routes.WORKSPACE) {
                                    // Treat this button like a tab switch
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                navController.navigate(Routes.LOGIN)
                            }
                        },
                    )
                }

                composable(Routes.WORKSPACE) {
                    if (!isLoggedIn) {
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    } else {
                        WorkspaceScreen(
                            viewModel = viewModel,
                            navController,
                            currentCredits
                        )
                    }
                }

                composable(Routes.PRICING) {
                    PricingScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                        onInitiatePurchase = onInitiatePurchase
                    )
                }

                // --- PROFILE & LOGOUT ROUTE ---
                composable(Routes.PROFILE) {
                    if (!isLoggedIn) {
                        LaunchedEffect(Unit) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        }
                    } else {
                        ProfileScreen(
                            viewModel = viewModel,
                            onLogout = {
                                // 1. Wipe the token from device storage & memory
                                viewModel.logout()

                                // 2. Navigate back to Home and clear the stack to avoid back-nav to profile
                                navController.navigate(Routes.HOME) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                }
                            },
                            onNavigateToContactUs = {
                                navController.navigate(Routes.CONTACT_US)
                            },
                            onNavigateToBilling = {
                                navController.navigate(Routes.BILLING)
                            },
                            onNavigateToFAQ = {
                                navController.navigate(Routes.FAQ)
                            },
                            onNavigateToMyProjects = {
                                navController.navigate(Routes.MY_PROJECTS)
                            },
                            onNavigateToEditProfile = {
                                navController.navigate(Routes.EDIT_PROFILE)
                            }
                        )
                    }
                }

                composable(Routes.MY_PROJECTS) {
                    MyProjectsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.EDIT_PROFILE) {
                    EditProfileScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.CONTACT_US) {
                    ContactUsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.BILLING) {
                    BillingScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onViewPricing = {
                            navController.navigate(Routes.PRICING) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Routes.FAQ) {
                    FAQScreen(
                        onBack = { navController.popBackStack() },
                        onNavigateToContact = { navController.navigate(Routes.CONTACT_US) }
                    )
                }
            }
        }

        // Plus Selection Modal Overlay
        AnimatedVisibility(
            visible = showPlusOverlay,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        ) {
            PlusButtonModalSheet(
                hazeState = hazeState,
                onDismiss = { showPlusOverlay = false },
                onSelectMode = { mode ->
                    showPlusOverlay = false
                    if (isLoggedIn) {
                        // TELL THE VIEWMODEL WHICH TAB TO OPEN
                        viewModel.setActiveSettingsTab(mode)

                        if (mode == "Basic") {
                            viewModel.setMotionPath("Circular Orbit")
                        } else {
                            viewModel.setMotionPath("Wave / Sway")
                        }

                        navController.navigate(Routes.WORKSPACE) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(Routes.LOGIN)
                    }
                }
            )
        }
    }
}

/**
 * Helper function generating a secure SHA-256 string for OAuth operations.
 */
private fun generateSecureNonce(): String {
    val rawNonce = UUID.randomUUID().toString()
    val bytes = rawNonce.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}



































