package com.quiet.launcher

import java.time.LocalDate

data class Verse(val text: String, val ref: String)

/** A verse a day (King James Version, public domain). */
object Verses {
    private val all = listOf(
        Verse("Trust in the LORD with all thine heart; and lean not unto thine own understanding.", "Proverbs 3:5"),
        Verse("In all thy ways acknowledge him, and he shall direct thy paths.", "Proverbs 3:6"),
        Verse("I can do all things through Christ which strengtheneth me.", "Philippians 4:13"),
        Verse("Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God.", "Philippians 4:6"),
        Verse("The LORD is my shepherd; I shall not want.", "Psalm 23:1"),
        Verse("Be still, and know that I am God.", "Psalm 46:10"),
        Verse("This is the day which the LORD hath made; we will rejoice and be glad in it.", "Psalm 118:24"),
        Verse("Thy word is a lamp unto my feet, and a light unto my path.", "Psalm 119:105"),
        Verse("So teach us to number our days, that we may apply our hearts unto wisdom.", "Psalm 90:12"),
        Verse("Commit thy works unto the LORD, and thy thoughts shall be established.", "Proverbs 16:3"),
        Verse("Whatsoever thy hand findeth to do, do it with thy might.", "Ecclesiastes 9:10"),
        Verse("And whatsoever ye do, do it heartily, as to the Lord, and not unto men.", "Colossians 3:23"),
        Verse("Set your affection on things above, not on things on the earth.", "Colossians 3:2"),
        Verse("See then that ye walk circumspectly, not as fools, but as wise, redeeming the time, because the days are evil.", "Ephesians 5:15–16"),
        Verse("But seek ye first the kingdom of God, and his righteousness; and all these things shall be added unto you.", "Matthew 6:33"),
        Verse("Come unto me, all ye that labour and are heavy laden, and I will give you rest.", "Matthew 11:28"),
        Verse("For God hath not given us the spirit of fear; but of power, and of love, and of a sound mind.", "2 Timothy 1:7"),
        Verse("Study to shew thyself approved unto God, a workman that needeth not to be ashamed, rightly dividing the word of truth.", "2 Timothy 2:15"),
        Verse("Be strong and of a good courage; be not afraid, neither be thou dismayed: for the LORD thy God is with thee whithersoever thou goest.", "Joshua 1:9"),
        Verse("They that wait upon the LORD shall renew their strength; they shall mount up with wings as eagles; they shall run, and not be weary; and they shall walk, and not faint.", "Isaiah 40:31"),
        Verse("Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee.", "Isaiah 41:10"),
        Verse("For I know the thoughts that I think toward you, saith the LORD, thoughts of peace, and not of evil, to give you an expected end.", "Jeremiah 29:11"),
        Verse("And we know that all things work together for good to them that love God, to them who are the called according to his purpose.", "Romans 8:28"),
        Verse("And be not conformed to this world: but be ye transformed by the renewing of your mind.", "Romans 12:2"),
        Verse("All things are lawful unto me, but all things are not expedient: all things are lawful for me, but I will not be brought under the power of any.", "1 Corinthians 6:12"),
        Verse("Let us not be weary in well doing: for in due season we shall reap, if we faint not.", "Galatians 6:9"),
        Verse("Whatsoever things are true, whatsoever things are honest, whatsoever things are just, whatsoever things are pure, whatsoever things are lovely… think on these things.", "Philippians 4:8"),
        Verse("I will set no wicked thing before mine eyes.", "Psalm 101:3"),
        Verse("Turn away mine eyes from beholding vanity; and quicken thou me in thy way.", "Psalm 119:37"),
        Verse("Keep thy heart with all diligence; for out of it are the issues of life.", "Proverbs 4:23"),
        Verse("Let thine eyes look right on, and let thine eyelids look straight before thee.", "Proverbs 4:25"),
        Verse("He becometh poor that dealeth with a slack hand: but the hand of the diligent maketh rich.", "Proverbs 10:4"),
        Verse("In all labour there is profit: but the talk of the lips tendeth only to penury.", "Proverbs 14:23"),
        Verse("Seest thou a man diligent in his business? he shall stand before kings.", "Proverbs 22:29"),
        Verse("The steps of a good man are ordered by the LORD: and he delighteth in his way.", "Psalm 37:23"),
        Verse("Delight thyself also in the LORD; and he shall give thee the desires of thine heart.", "Psalm 37:4"),
        Verse("Wait on the LORD: be of good courage, and he shall strengthen thine heart: wait, I say, on the LORD.", "Psalm 27:14"),
        Verse("Casting all your care upon him; for he careth for you.", "1 Peter 5:7"),
        Verse("Draw nigh to God, and he will draw nigh to you.", "James 4:8"),
        Verse("If any of you lack wisdom, let him ask of God, that giveth to all men liberally, and upbraideth not; and it shall be given him.", "James 1:5"),
        Verse("Therefore if any man be in Christ, he is a new creature: old things are passed away; behold, all things are become new.", "2 Corinthians 5:17"),
        Verse("Peace I leave with you, my peace I give unto you: not as the world giveth, give I unto you. Let not your heart be troubled, neither let it be afraid.", "John 14:27"),
        Verse("I am the vine, ye are the branches: He that abideth in me, and I in him, the same bringeth forth much fruit: for without me ye can do nothing.", "John 15:5"),
        Verse("His compassions fail not. They are new every morning: great is thy faithfulness.", "Lamentations 3:22–23"),
        Verse("Watch and pray, that ye enter not into temptation: the spirit indeed is willing, but the flesh is weak.", "Matthew 26:41"),
        Verse("Let us lay aside every weight, and the sin which doth so easily beset us, and let us run with patience the race that is set before us.", "Hebrews 12:1"),
        Verse("Let the words of my mouth, and the meditation of my heart, be acceptable in thy sight, O LORD, my strength, and my redeemer.", "Psalm 19:14"),
        Verse("Create in me a clean heart, O God; and renew a right spirit within me.", "Psalm 51:10"),
        Verse("Except the LORD build the house, they labour in vain that build it.", "Psalm 127:1"),
        Verse("The fear of the LORD is the beginning of wisdom.", "Proverbs 9:10"),
    )

    fun today(): Verse = all[(LocalDate.now().toEpochDay() % all.size).toInt()]
}
