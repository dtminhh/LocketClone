package com.mainpack.locketcameraclone.feature.onboarding.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.mainpack.locketcameraclone.R
import com.mainpack.locketcameraclone.databinding.FragmentSignUpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {

    /**
     * Another way to back to previous screen without using navigation component
     * */
//    enum class SignUpState {
//        EMAIL,
//        PASSWORD
//    }

//    private var currentState = SignUpState.EMAIL

    private lateinit var binding: FragmentSignUpBinding
    private val signUpViewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpObserver()
        setUpListener()
    }

    private fun setUpObserver() {
        signUpViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.apply {
                signUpNextToNameStepBtn.isEnabled = state.isPasswordValid
            }
        }
    }

    private fun setUpListener() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.signUpBackBtn.performClick()
            }
        })
        binding.signUpContinueBtn.setOnClickListener {
            goToPasswordScreen()
        }

        binding.signUpNextToNameStepBtn.setOnClickListener {
            goToNameScreen()
        }
        binding.signUpBackBtn.setOnClickListener {
            when (binding.signUpFlipper.displayedChild) {
                1 -> {
                    backToEmailScreen()
                }

                2 -> {
                    goToPasswordScreen()
                }

                else -> findNavController().popBackStack()
            }
        }

        binding.signUpConfirmBtn.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_onboardingFragment)
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(REQUEST_RESULT to true)
            )
        }

        val triggerCheck = {
            val password = binding.passwordEdt.text.toString()
            val confirmPassword = binding.passwordValidateEdt.text.toString()
            signUpViewModel.onPasswordInputChanged(password, confirmPassword)
        }
        binding.passwordEdt.onTextChange { triggerCheck() }
        binding.passwordValidateEdt.onTextChange { triggerCheck() }

        /**
         * Another way to back to previous screen without using navigation component
         * */
//           binding.signUpContinueBtn.setOnClickListener {
//            if (currentState == SignUpState.EMAIL) {
//                passwordProcess()
//            }
//        }
//        binding.signUpBackBtn.setOnClickListener {
//            if (currentState == SignUpState.PASSWORD) {
//                emailProcess()
//            } else findNavController().popBackStack()
//        }
    }

    /**
     * Another way to back to previous screen without using navigation component
     * */
//    private fun emailProcess() {
//        binding.apply {
//            TransitionManager.beginDelayedTransition(root)
//
//            currentState = SignUpState.EMAIL
//            signUpTitleTxt.text = getString(R.string.what_s_your_email)
//            signUpEmailEdt.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS
//            signUpPhoneNumberBtn.visibility = VISIBLE
//            signUpPasswordRequireTxt.visibility = GONE
//            signUpContinueBtn.text = getString(R.string.continue_string)
//            signUpEmailEdt.text = null
//            signUpEmailEdt.hint = getString(R.string.email_address)
//        }
//
//    }

    /**
     * Another way to back to previous screen without using navigation component
     * */
//    private fun passwordProcess() {
//        binding.apply {
//            TransitionManager.beginDelayedTransition(root)
//
//            currentState = SignUpState.PASSWORD
//            signUpTitleTxt.text = getString(R.string.type_password_string)
//            signUpEmailEdt.inputType = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_PASSWORD
//            signUpEmailEdt.hint = getString(R.string.password_hidden)
//            signUpEmailEdt.text = null
//
//            signUpPhoneNumberBtn.visibility = GONE
//            signUpPasswordRequireTxt.visibility = VISIBLE
//
//            signUpContinueBtn.text = getString(R.string.confirm)
//
//        }
//    }

    private fun goToPasswordScreen() {
        binding.apply {
            passwordValidateEdt.text = null
            passwordEdt.text = null
            signUpFlipper.setInAnimation(context, R.anim.slide_in_right_navigator)

            signUpFlipper.displayedChild = 1

            passwordEdt.requestFocus()
        }
    }

    private fun backToEmailScreen() {
        binding.apply {
            signUpEmailEdt.text = null
            signUpFlipper.setInAnimation(context, R.anim.slide_out_left_navigator)

            signUpFlipper.displayedChild = 0
        }
    }

    private fun goToNameScreen() {
        binding.apply {
            firstNameEdt.text = null
            lastNameEdt.text = null
            signUpFlipper.setInAnimation(context, R.anim.slide_in_right_navigator)
            signUpFlipper.displayedChild = 2

            firstNameEdt.requestFocus()
        }
    }

    private fun EditText.onTextChange(action: (String) -> Unit) {
        this.addTextChangedListener { action(it.toString()) }
    }
}
