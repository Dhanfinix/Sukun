document.addEventListener('DOMContentLoaded', () => {
    // Mobile Navigation Toggle
    const mobileMenuBtn = document.querySelector('.mobile-menu-btn');
    const navLinks = document.querySelector('.nav-links');
    
    if (mobileMenuBtn && navLinks) {
        mobileMenuBtn.addEventListener('click', () => {
            navLinks.classList.toggle('active');
            
            // Toggle icon
            const icon = mobileMenuBtn.querySelector('.material-symbols-rounded');
            if (navLinks.classList.contains('active')) {
                icon.textContent = 'close';
            } else {
                icon.textContent = 'menu';
            }
        });
    }

    // Close mobile menu when clicking a link
    const links = document.querySelectorAll('.nav-links a');
    links.forEach(link => {
        link.addEventListener('click', () => {
            if (navLinks.classList.contains('active')) {
                navLinks.classList.remove('active');
                const icon = mobileMenuBtn.querySelector('.material-symbols-rounded');
                icon.textContent = 'menu';
            }
        });
    });

    // Add scroll effect to navbar
    const navbar = document.querySelector('.navbar');
    
    window.addEventListener('scroll', () => {
        if (window.scrollY > 10) {
            navbar.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.5)';
            navbar.style.backgroundColor = 'rgba(20, 18, 24, 0.95)';
        } else {
            navbar.style.boxShadow = 'none';
            navbar.style.backgroundColor = 'rgba(20, 18, 24, 0.8)';
        }
    });

    // Mockup Simulation
    const prayerCards = document.querySelectorAll('.prayer-card');
    
    // Simple mock animation switching active states
    let activeIndex = 0;
    
    setInterval(() => {
        // Reset all
        prayerCards.forEach(card => card.classList.remove('active'));
        
        // Set new active
        activeIndex = (activeIndex + 1) % prayerCards.length;
        prayerCards[activeIndex].classList.add('active');
        
        // Update time
        const timeHeader = document.querySelector('.status-bar .time');
        const now = new Date();
        const hours = String(now.getHours() % 12 || 12).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        
        if (timeHeader) {
            timeHeader.textContent = `${hours}:${minutes}`;
        }
    }, 5000); // Switch active state every 5 seconds for demo
});
