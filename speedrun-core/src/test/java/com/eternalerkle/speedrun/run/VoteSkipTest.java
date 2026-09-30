package com.eternalerkle.speedrun.run;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteSkipTest {
	@Test
	void neededIsMoreThanHalf() {
		assertEquals(1, VoteSkip.needed(1));
		assertEquals(2, VoteSkip.needed(2));
		assertEquals(2, VoteSkip.needed(3));
		assertEquals(3, VoteSkip.needed(4));
		assertEquals(3, VoteSkip.needed(5));
	}

	@Test
	void singlePlayerPassesInstantly() {
		VoteSkip votes = new VoteSkip();
		votes.vote(UUID.randomUUID());
		assertTrue(votes.passes(1));
	}

	@Test
	void halfIsNotEnough() {
		VoteSkip votes = new VoteSkip();
		votes.vote(UUID.randomUUID());
		assertFalse(votes.passes(2));
		votes.vote(UUID.randomUUID());
		assertTrue(votes.passes(2));
		assertFalse(votes.passes(4));
	}

	@Test
	void duplicateVotesAndClear() {
		VoteSkip votes = new VoteSkip();
		UUID player = UUID.randomUUID();
		assertTrue(votes.vote(player));
		assertFalse(votes.vote(player));
		assertEquals(1, votes.count());
		votes.onRunEnd(null);
		assertEquals(0, votes.count());
		assertFalse(votes.passes(0));
	}
}
