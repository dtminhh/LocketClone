package com.mainpack.locketcameraclone.ui.fragment.onBoarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.mainpack.locketcameraclone.R
import com.mainpack.locketcameraclone.databinding.FragmentOnboardingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingFragment : Fragment() {
    private lateinit var binding: FragmentOnboardingBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListener()
        setFragmentResultListener("SIGN_UP_RESULT") { _, bundle ->
            val signUpSuccess = bundle.getBoolean("success")
            if (signUpSuccess) {
                Toast.makeText(
                    context,
                    "You've sign up, please sign in to continue",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setUpListener() {
        binding.onBoardingSignUpBtn.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingFragment_to_signUpFragment2)
        }

        binding.signInBtn.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingFragment_to_signInFragment)
        }
    }
}