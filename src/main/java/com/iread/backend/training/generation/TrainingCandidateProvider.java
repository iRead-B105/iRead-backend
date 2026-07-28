package com.iread.backend.training.generation;

public interface TrainingCandidateProvider {

    TrainingCandidateResponse generate(TrainingCandidateRequest request);
}
