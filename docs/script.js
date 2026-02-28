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


});
