package com.example.passkey.app

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.passkey.app.databinding.FragmentSignInBinding
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {

    private lateinit var credentialManager: CredentialManager
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!
    private lateinit var listener: SignInFragmentCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as SignInFragmentCallback
        } catch (castException: ClassCastException) {
            /** The activity does not implement the listener.  */
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        credentialManager = CredentialManager.create(requireActivity())

        binding.signInWithSavedCredentials.setOnClickListener(signInWithSavedCredentials())
    }

    private fun signInWithSavedCredentials(): View.OnClickListener {
        return View.OnClickListener {

            lifecycleScope.launch {
                configureViews(View.VISIBLE, false)

                //TODO : Call getSavedCredentials() method to signin using passkey/password

                val data = getSavedCredentials()

                configureViews(View.INVISIBLE, true)

              //TODO : complete the authentication process after validating the public key credential to your server and let the user in.

                data?.let {
                    sendSignInResponseToServer()
                    listener.showHome()
                }
            }
        }
    }

    private fun configureViews(visibility: Int, flag: Boolean) {
        configureProgress(visibility)
        binding.signInWithSavedCredentials.isEnabled = flag
    }

    private fun configureProgress(visibility: Int) {
        binding.textProgress.visibility = visibility
        binding.circularProgressIndicator.visibility = visibility
    }

    private fun fetchAuthJsonFromServer(): String {
        //TODO fetch authentication mock json
        return requireContext().readFromAsset("AuthFromServer")
    }

    private fun sendSignInResponseToServer(): Boolean {
        return true
    }

    private suspend fun getSavedCredentials(): String? {

        //TODO create a GetPublicKeyCredentialOption() with necessary authentication json from server

        val getPublicKeyCredentialOption =
            GetPublicKeyCredentialOption(fetchAuthJsonFromServer(), null, true)

        //TODO create a PasswordOption to retrieve all the associated user's password
        val getPasswordOption = GetPasswordOption()

        //TODO call getCredential() with required credential options
        val result = try {
            credentialManager.getCredential(
                GetCredentialRequest(
                    listOf(
                        getPublicKeyCredentialOption,
                        getPasswordOption
                    )
                ),
                requireActivity()
            )
        } catch (e: Exception) {
            configureViews(View.INVISIBLE, true)
            Log.e("Auth", "getCredential failed with exception: " + e.message.toString())
            activity?.showErrorAlert(
                "An error occurred while authenticating through saved credentials. Check logs for additional details"
            )
            return null
        }

        if (result.credential is PublicKeyCredential) {
            val cred = result.credential as PublicKeyCredential
            DataProvider.setSignedInThroughPasskeys(true)
            return "Passkey: ${cred.authenticationResponseJson}"
        }
        if (result.credential is PasswordCredential) {
            val cred = result.credential as PasswordCredential
            DataProvider.setSignedInThroughPasskeys(false)
            return "Got Password - User:${cred.id} Password: ${cred.password}"
        }

        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        configureProgress(View.INVISIBLE)
        _binding = null
    }

    interface SignInFragmentCallback {
        fun showHome()
    }
}
