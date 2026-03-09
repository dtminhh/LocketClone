package com.mainpack.locketcameraclone.feature.onboarding.presentation

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
        setFragmentResultListener(REQUEST_KEY) { _, bundle ->
            val signUpSuccess = bundle.getBoolean(REQUEST_RESULT)
            if (signUpSuccess) {
                Toast.makeText(
                    context,
                    getString(R.string.you_ve_sign_up_please_sign_in_to_continue),
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
